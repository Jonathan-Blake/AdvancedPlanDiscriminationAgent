package blake.bot.titfortat;

import blake.bot.analyser.*;
import blake.bot.suppliers.DealGenerator;
import blake.bot.suppliers.PrioritisedProposalSupplierList;
import blake.bot.utility.HashedPower;
import blake.bot.utility.Relationship;
import blake.bot.utility.RelationshipMatrix;
import blake.bot.utility.Utility;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DiplomacyProposal;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AdvancedPlanDiscriminationAgent extends AbstractNegotiationLoopNegotiator {

    public static final double DISLIKE_PROBABILITY_IF_FALSE = 0.9;
    public static final double DISLIKE_LIKELIHOOD = 0.1;
    public static final double LIKE_PROBABILITY_IF_FALSE = 0.3;
    public static final double LIKE_LIKELIHOOD = 0.7;
    private final PlanCache planCache;
    private DealGenerator proposalSupplier;
    private boolean isFirstTurn = true;
    private RelationshipMatrix<Double> relationshipMatrix;
    private List<Province> previouslyOwned;

    AdvancedPlanDiscriminationAgent(String[] args) {
        super(args);
        this.planCache = new PlanCache(this.getTacticalModule());
    }

    public static void main(String[] args) {
        AdvancedPlanDiscriminationAgent myPlayer = new AdvancedPlanDiscriminationAgent(args);
        myPlayer.run();
    }

    @Override
    public void negotiate(long negotiationDeadline) {
        this.planCache.setNextTurn(this.getGame(), this.getMe(), this.getConfirmedDeals());
        this.planCache.setNoDealPlan(this.planCache.analysePlan(
                new PlanInfo(
                        this.game,
                        new HashedPower(this.getMe()),
                        this.getConfirmedDeals(),
                        new BasicDeal(Collections.emptyList(), Collections.emptyList()))));
        super.negotiate(negotiationDeadline);
        this.isFirstTurn = false;
        this.proposalSupplier = null;
        this.previouslyOwned = this.getMe().getOwnedSCs();
        this.getLogger().logln("APDAgent adjudicator data: " + AdvancedAdjudicator.getData(), true);
        this.getLogger().logln(String.format("DumbotDecisions data: accepted = %d rejected = %d", PlanCache.getDumbBotAccepts(), PlanCache.getDumbBotRejects()), true);
    }

    @Override
    protected void handleRejectedMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        if (receivedMessage.getMessageId().contains(this.getMe().getName())) {
            //Rejected my proposal
        }
        this.planCache.removePlan(PlanInfoMatcher.dealId(receivedMessage.getMessageId()));
        this.getLogger().logln("APDAgent.negotiate() Received rejection from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected void handleConfirmationMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        this.planCache.setNoDealPlan(this.planCache.analysePlan(
                new PlanInfo(
                        this.game,
                        new HashedPower(this.getMe()),
                        this.getConfirmedDeals(),
                        new BasicDeal(Collections.emptyList(), Collections.emptyList()))));
        this.planCache.removePlan(PlanInfoMatcher.stillConsistent(this.getConfirmedDeals()).negate());
        this.getLogger().logln("APDAgent.negotiate() Received confirmation from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected void handleProposalMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
        BasicDeal deal = (BasicDeal) receivedProposal.getProposedDeal();
        this.getLogger().logln("APDAgent.negotiate() Received proposal from " + receivedMessage.getSender() + ": " + receivedProposal);

        if (Utility.Plans.testConsistency(deal, this.getGame(), this.getConfirmedDeals())) {
            AnalysedPlan newPlan = planCache.analysePlan(
                    new PlanInfo(
                            this.game,
                            new HashedPower(this.getMe()),
                            this.getConfirmedDeals(),
                            deal
                    )
            );
            newPlan.getInfo().setDealId(receivedProposal.getId());
            if (planCache.betterThanNoDeal(newPlan)) {
                this.acceptAndClearInconsistentPlans(receivedProposal.getId(), receivedMessage.getSender(), Utility.Lists.append(this.getConfirmedDeals(), deal));
                this.getLogger().logln("APDAgent.negotiate() Accepted proposal from " + receivedMessage.getSender() + ": " + receivedProposal, true);
            } else if (newPlan.getDBraneValue() - planCache.getNoDealAnalysedPlan().getDBraneValue() == 0
                    && this.planCache.getNoDealPlan().getMyOrders().containsAll(newPlan.getPlan().getMyOrders())
                    && this.getAllies().contains(this.game.getPower(receivedMessage.getSender()))
            ) {
                this.getLogger().logln("APDAgent.negotiate() Accepted proposal (same orders) from " + receivedMessage.getSender() + ": " + receivedProposal, true);
                this.acceptAndClearInconsistentPlans(receivedProposal.getId(), receivedMessage.getSender(), Utility.Lists.append(this.getConfirmedDeals(), deal));
            } else {
                this.getLogger().logln("APDAgent.negotiate() Rejected proposal from " + receivedMessage.getSender() + ": " + receivedProposal);
                this.rejectAndDislikeProposal(receivedProposal.getId(), receivedMessage.getSender());
            }
        } else {
            this.getLogger().logln("APDAgent.negotiate() Rejected proposal from " + receivedMessage.getSender() + ": " + receivedProposal);
            this.rejectAndDislikeProposal(receivedProposal.getId(), receivedMessage.getSender());
            this.rejectProposal(receivedProposal.getId());
        }
    }

    private void acceptAndClearInconsistentPlans(String id, String sender, List<BasicDeal> newCommitments) {
        this.acceptProposal(id);
        this.like(sender);
        this.planCache.removePlan(PlanInfoMatcher.stillConsistent(newCommitments).negate());
    }

    private void rejectAndDislikeProposal(String id, String sender) {
        this.dislike(sender);
        this.rejectProposal(id);
    }


    @Override
    protected void handleAcceptanceMessage(Message receivedMessage) {
        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();

        this.getLogger().logln("APDAgent.negotiate() Received acceptance from " + receivedMessage.getSender() + ": " + receivedProposal);
    }

    @Override
    protected DealGenerator getProposalSupplier() {
        if (this.proposalSupplier == null) {
            this.proposalSupplier = new PrioritisedProposalSupplierList(
//                new PeaceDealSupplier(
//                        true,
//                        allies,
//                        this.getMe(),
//                        this.getGame()
//                )
//                new PeaceDealSupplier(
//                        this.isFirstTurn,
//                        allies,
//                        this.getMe(),
//                        this.getGame()
//                )
//				,
//				new MutualSupportSupplier(
//						this.getConfirmedDeals(),
//						this.getTacticalModule(),
//						this.getGame(),
//						this.getMe(),
//						this.getNegotiatingPowers(),
//						this.getLogger()
//				)
////                ,
//                    new PlanBasedSupplier(
//                            this.getMe(),
//                            this.planChache.getNoDealPlan(),
//                            this.getTacticalModule(),
//                            this.getGame(),
//                            this.getConfirmedDeals(),
//                            allies
//                    )
//				,
//                    new CombinedAttackSupplier(
//                            this.getMe(),
//                            allies,
//                            this.getNegotiatingPowers(),
//                            this.getGame(),
//                            this.getTacticalModule(),
//                            this.getBestPlan(),
//                            this.getConfirmedDeals()
//                    )
            );
        }
        return this.proposalSupplier;
    }

    @Override
    public void start() {
        //Inherited but not used.
    }

    @Override
    public void receivedOrder(Order order) {
        if (order instanceof MTOOrder && order.getPower() != this.getMe()) {
            MTOOrder mtoOrder = (MTOOrder) order;
            if (this.previouslyOwned == null) {
                this.getLogger().logln("PreviouslyOwned is null?????", true);
                return;
            }
            if (mtoOrder.getDestination() == null) {
                this.getLogger().logln("__________________________________________Somehow MTO has no destination?????", true);
                return;
            }
            if (this.previouslyOwned.contains(mtoOrder.getDestination().getProvince())) {
                if (this.getAllies().contains(mtoOrder.getPower())) {
                    //Dislike or break alliance, whichever is smaller.
                    Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(mtoOrder.getPower());
                    currentRelationship.ifPresent(curr -> this.getRelationshipMatrix().setRelationship(mtoOrder.getPower(),
                            Math.min(
                                    0.25d,
                                    Utility.Probability.bayes(
                                            curr,
                                            DISLIKE_PROBABILITY_IF_FALSE,
                                            DISLIKE_LIKELIHOOD
                                    ))));
                } else {
                    this.dislike(mtoOrder.getPower());
                }
            }
        }
    }

    private List<Power> getAllies() {
        return this.getRelationshipMatrix().getAllies();
    }

    private RelationshipMatrix<Double> getRelationshipMatrix() {
        if (this.relationshipMatrix == null) {
            this.relationshipMatrix = RelationshipMatrix.getDoubleMatrix(this.getMe(), this.getNegotiatingPowers(), this.getGame(), 0.75, (value -> {
                if (value >= 0.5) {
                    return Relationship.ALLIED;
                } else {
                    return Relationship.WAR;
                }
            }));
        }
        return this.relationshipMatrix;
    }

    private void dislike(String power) {
        dislike(this.getGame().getPower(power));
    }

    private void dislike(Power power) {
        Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(power);
        currentRelationship.ifPresent(curr -> {
            this.getRelationshipMatrix().setRelationship(power,
                    Math.max(
                            0.000001d,
                            Utility.Probability.bayes(
                                    curr,
                                    DISLIKE_PROBABILITY_IF_FALSE,
                                    DISLIKE_LIKELIHOOD
                            )));
            this.getLogger().logln(String.format("Tit for Tat Agent dislikes %s : %s -> %s", power.getName(), currentRelationship.get(), this.getRelationshipMatrix().getRelationship(power)));
        });
    }

    private void like(String power) {
        like(this.getGame().getPower(power));
    }

    private void like(Power power) {
        Optional<Double> currentRelationship = this.getRelationshipMatrix().getRelationship(power);
        currentRelationship.ifPresent(curr -> {
            this.getRelationshipMatrix().setRelationship(power,
                    Math.min(
                            0.999999d,
                            Utility.Probability.bayes(
                                    curr,
                                    LIKE_PROBABILITY_IF_FALSE,
                                    LIKE_LIKELIHOOD
                            )));
            this.getLogger().logln(String.format("Tit for Tat Agent likes %s : %s -> %s", power.getName(), currentRelationship.get(), this.getRelationshipMatrix().getRelationship(power)));
        });
    }
}

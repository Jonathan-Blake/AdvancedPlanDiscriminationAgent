package blake.bot.titfortat;

import blake.bot.suppliers.DealGenerator;
import ddejonge.bandana.anac.ANACNegotiator;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;

import java.util.Iterator;

public abstract class AbstractNegotiationLoopNegotiator extends ANACNegotiator {

    AbstractNegotiationLoopNegotiator(String[] args) {
        super(args);
    }

    @Override
    public void negotiate(long negotiationDeadline) {
        negotiationLoop(negotiationDeadline);
    }

    private void negotiationLoop(long negotiationDeadline) {
        int loopSinceMessage = 0;
        while (System.currentTimeMillis() < negotiationDeadline) {
            if (this.hasMessage()) {
                loopSinceMessage = 0;
                Message receivedMessage = this.removeMessageFromQueue();
                this.getLogger().logln("got message " + receivedMessage.getContent(), true);

                switch (receivedMessage.getPerformative()) {
                    case "ACCEPT":
                        handleAcceptanceMessage(receivedMessage);
                        break;
                    case "PROPOSE":
                        handleProposalMessage(receivedMessage);
                        break;
                    case "CONFIRM":
                        handleConfirmationMessage(receivedMessage);
                        break;
                    case "REJECT":
                        handleRejectedMessage(receivedMessage);
                        break;
                    default:
                        this.getLogger().logln("Unexpected message performative : " + receivedMessage.getPerformative());
                }
            } else {
                final Iterator<BasicDeal> iterator = this.getProposalSupplier().iterator();
                if (iterator.hasNext()) {
                    this.proposeDealAndLog(iterator.next());
                }
                if (++loopSinceMessage >= 5) {
                    //break if more than a second and no more deals are accepted or offered
                    break;
                } else {
                    sleep(200L);
                }
            }
        }
    }

    private void proposeDealAndLog(BasicDeal deal) {
        boolean containsOtherPower = !deal.getDemilitarizedZones().isEmpty() && deal.getDemilitarizedZones().stream().anyMatch(
                commitment -> commitment.getPowers().size() > 1 || !commitment.getPowers().get(0).getName().equals(this.getMe().getName()));
        boolean validOrderTypes = true;
        for (OrderCommitment commitment : deal.getOrderCommitments()) {
            if (!commitment.getOrder().getPower().getName().equals(this.getMe().getName())) {
                containsOtherPower = true;
            }

            if (!(commitment.getOrder() instanceof HLDOrder) && !(commitment.getOrder() instanceof MTOOrder) && !(commitment.getOrder() instanceof SUPOrder) && !(commitment.getOrder() instanceof SUPMTOOrder)) {
                validOrderTypes = false;
                break;
            }
        }

        if (!containsOtherPower) {
            this.getLogger().logln("Error! The proposed deal is not valid! A deal must involve at least one power other than yourself. " + deal, true);
        } else if (!validOrderTypes) {
            this.getLogger().logln("Error! In the ANAC competition you can only propose deals that involve orders of type HLDOrder, MTOOrder, SUPOrder or SUPMTOOrder " + deal, true);
        } else {
            this.proposeDeal(deal);
            this.getLogger().logln("Proposed deal " + deal);
        }

    }

    protected abstract void handleRejectedMessage(Message receivedMessage);

    protected abstract void handleConfirmationMessage(Message receivedMessage);

    protected abstract void handleProposalMessage(Message receivedMessage);

    protected abstract void handleAcceptanceMessage(Message receivedMessage);

    public Power getMe() {
        return this.me;
    }

    public Game getGame() {
        return this.game;
    }

    protected abstract DealGenerator getProposalSupplier();

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

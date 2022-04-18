package blake.bot.analyser;

import blake.bot.utility.DatedObject;
import blake.bot.utility.HashedPower;
import blake.bot.utility.JavaDumbbot;
import blake.bot.utility.Utility;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;

import java.util.*;
import java.util.function.Predicate;

public class PlanCache {

    private static int dumbBotAccepts = 0;
    private static int dumbBotRejects = 0;
    private final DBraneTactics tactics;
    Power me;
    Game game;
    AnalysedPlan noDealPlan;
    private List<BasicDeal> commitments;
    private JavaDumbbot dumbBotAnalysis;
    private final Comparator<AnalysedPlan> planComparator = (o1, o2) -> {
        int dbraneComp = Comparator.comparing(AnalysedPlan::getDBraneValue).compare(o1, o2);
        return dbraneComp != 0 ?
                dbraneComp :
                Comparator.<AnalysedPlan>comparingInt(plan -> plan.getDumbbotValue(currentDumbotValues())).compare(o1, o2);
    };
    SortedMap<AnalysedPlan, PlanInfo> plans = new TreeMap<>(planComparator);

    public PlanCache(DBraneTactics tactics) {
        this.tactics = tactics;
    }

    public static int getDumbBotAccepts() {
        return dumbBotAccepts;
    }

    public static void setDumbBotAccepts(int dumbBotAccepts) {
        PlanCache.dumbBotAccepts = dumbBotAccepts;
    }

    public static int getDumbBotRejects() {
        return dumbBotRejects;
    }

    public static void setDumbBotRejects(int dumbBotRejects) {
        PlanCache.dumbBotRejects = dumbBotRejects;
    }

    public AnalysedPlan analysePlan(PlanInfo planInfo) {
        AnalysedPlan ret = new AnalysedPlan(
                this.tactics.determineBestPlan(
                        planInfo.getGame(),
                        planInfo.getMe().asPower(),
                        planInfo.getCommitments(),
                        planInfo.getAlliesAsPower()),
                planInfo);
        if (ret.getPlan() != null && Utility.Plans.testConsistency(planInfo.getDeal(), this.game, planInfo.getCommitments())) {
            plans.put(ret, planInfo);
            return ret;
        } else {
            return null;
        }
    }

    public AnalysedPlan lookupDeal(BasicDeal basicDeal) {
        return plans.entrySet().stream()
                .filter(entry -> entry.getValue().getDeal().equals(basicDeal)).map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> analysePlan(new PlanInfo(game, new HashedPower(me), commitments, basicDeal)));
    }

    public boolean betterThanNoDeal(AnalysedPlan proposal) {
        final AnalysedPlan currentPlan = this.getNoDealAnalysedPlan();
        int dbraneComp = Comparator.comparing(AnalysedPlan::getDBraneValue).compare(proposal, currentPlan);
        int ret;
        if (dbraneComp > 0) {
            ret = dbraneComp;
            System.out.println(" following DBrane comp " + proposal.getDBraneValue() + " " + currentPlan.getDBraneValue());
        } else {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            ret = Integer.compare(
//            		proposal.getDumbbotValue(this.dumbBotAnalysis),
//            		(int) (currentPlan.getDumbbotValue(this.dumbBotAnalysis) *1.0)
//            		);
                    tactics.determineBestPlan(
                            AdvancedAdjudicator.advanceToMovementPhase(proposal.getExpectedResult()),
                            this.me,
                            proposal.getInfo().getCommitments()
                    ).getValue(),
                    tactics.determineBestPlan(
                            AdvancedAdjudicator.advanceToMovementPhase(currentPlan.getExpectedResult()),
                            this.me,
                            currentPlan.getInfo().getCommitments()).getValue());
            if (ret > 0) {
                setDumbBotAccepts(getDumbBotAccepts() + 1);
            } else {
                setDumbBotRejects(getDumbBotRejects() + 1);
            }
        }

        return ret > 0;
    }

    private JavaDumbbot calculateDumbbot(Game expectedResult) {
        return new JavaDumbbot(expectedResult, this.me, false);
    }

    private Map<Region, Integer> currentDumbotValues() {
        return this.dumbBotAnalysis.getDestinationValue();
    }

    public Comparator<BasicDeal> getDealComparator() {
        return (o1, o2) -> {
            AnalysedPlan o1Plan = lookupDeal(o1);
            AnalysedPlan o2Plan = lookupDeal(o2);

            return this.planComparator.compare(o1Plan, o2Plan);
        };
    }

    public void setNextTurn(Game game, Power me, List<BasicDeal> confirmedDeals) {
        this.game = game;
        this.me = me;
        this.commitments = confirmedDeals;
        this.dumbBotAnalysis = new JavaDumbbot(this.game, this.me, true);
        this.removePlan(
                planInfo -> Utility.Dates.isHistory(new DatedObject(planInfo), new DatedObject(this.game))
        );
    }

    public void removePlan(Predicate<PlanInfo> planInfoPredicate) {
        Set<AnalysedPlan> plansToRemove = new HashSet<>();
        plans.forEach((analysedPlan, planInfo) -> {
            if (planInfoPredicate.test(planInfo)) {
                plansToRemove.add(analysedPlan);
            }
        });
        plansToRemove.forEach(plan -> plans.remove(plan));

    }

    public Plan getNoDealPlan() {
        return this.noDealPlan.getPlan();
    }

    public void setNoDealPlan(AnalysedPlan analysePlan) {
        this.noDealPlan = analysePlan;
    }

    public AnalysedPlan getNoDealAnalysedPlan() {
        return this.noDealPlan;
    }
}

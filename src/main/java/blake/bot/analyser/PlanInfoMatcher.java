package blake.bot.analyser;

import blake.bot.utility.Utility;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.List;
import java.util.function.Predicate;

public class PlanInfoMatcher {
    private PlanInfoMatcher() {
    }

    public static Predicate<PlanInfo> dealId(String messageId) {
        return planInfo -> planInfo.getDealId() != null && planInfo.getDealId().equals(messageId);
    }

    public static Predicate<PlanInfo> stillConsistent(List<BasicDeal> commitments) {
        return planInfo -> Utility.Plans.testConsistency(planInfo.getDeal(), planInfo.getGame(), commitments);
    }
}

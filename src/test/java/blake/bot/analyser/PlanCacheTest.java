package blake.bot.analyser;

import blake.bot.utility.HashedPower;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.gameBuilder.DiplomacyGameBuilder;
import ddejonge.bandana.negoProtocol.BasicDeal;
import es.csic.iiia.fabregues.dip.board.Region;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlanCacheTest {
    private final DBraneTactics mockTactics = new DBraneTactics();
    private final PlanCache planCache = new PlanCache(mockTactics);
    MockAnalysedPlan planA = new MockAnalysedPlan();
    MockAnalysedPlan planB = new MockAnalysedPlan();

    @Test
    public void testBetterThanNoDeal() {
        planCache.setNoDealPlan(planA);
        planCache.game = DiplomacyGameBuilder.createDefaultGame();

        planA.setDBraneValue(1);
        planB.setDBraneValue(0);
        assertFalse("Original Plan has higher DBrane value", planCache.betterThanNoDeal(planB));
        planA.setDBraneValue(0);
        planB.setDBraneValue(1);
        assertTrue("Compared Plan has higher DBrane value", planCache.betterThanNoDeal(planB));

        planA.setDBraneValue(0);
        planB.setDBraneValue(0);
        planA.setDumbbotValue(1000);
        planB.setDumbbotValue(900);
        assertFalse("Original Plan has higher DumbBot value", planCache.betterThanNoDeal(planB));

        planA.setDBraneValue(0);
        planB.setDBraneValue(0);
        planA.setDumbbotValue(1000);
        planB.setDumbbotValue(1000);
        assertFalse("Original Plan has equal DumbBot value", planCache.betterThanNoDeal(planB));

        planA.setDBraneValue(0);
        planB.setDBraneValue(0);
        planA.setDumbbotValue(900);
        planB.setDumbbotValue(1000);
        assertTrue("Comparison Plan has higher DumbBot value", planCache.betterThanNoDeal(planB));
    }

    private class MockAnalysedPlan extends AnalysedPlan {
        private int dumb;
        private int dbrane;

        MockAnalysedPlan(Plan plan, PlanInfo data) {
            super(plan, data);
        }

        MockAnalysedPlan() {
            super(null, null);
        }

        public void setDumbbotValue(int i) {
            this.dumb = i;
        }

        @Override
        public Integer getDBraneValue() {
            return this.dbrane;
        }

        public void setDBraneValue(int i) {
            this.dbrane = i;
        }

        @Override
        public int getDumbbotValue(Map<Region, Integer> destinationWeights) {
            return this.dumb;
        }

        @Override
        public PlanInfo getInfo() {
            return new PlanInfo(planCache.game, new HashedPower(planCache.game.getPower("ENG")), Collections.emptyList(), new BasicDeal(Collections.emptyList(), Collections.emptyList()));
        }
    }
}
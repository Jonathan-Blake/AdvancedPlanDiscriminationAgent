package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface DealGenerator extends Iterable<BasicDeal> {
    void reset(List<BasicDeal> confirmedDeals);

    void reset(List<BasicDeal> confirmedDeals, Plan newPlan);

    default Stream<BasicDeal> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

}

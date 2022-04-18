package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class FilteredProposalSupplier implements DealGenerator {

    private final DealGenerator supplier;
    private final Iterator<BasicDeal> iterator;
    private final Predicate<BasicDeal> predicate;

    public FilteredProposalSupplier(Predicate<BasicDeal> filter, DealGenerator supplier) {
        this.supplier = supplier;
        this.predicate = filter;
        iterator = new BasicDealIterator(this::get);

    }

    private BasicDeal get() {
        if (supplier.iterator().hasNext()) {
            BasicDeal ret = supplier.iterator().next();
            if (this.predicate.test(ret)) {
                return ret;
            } else {
                return get();
            }
        } else {
            return null;
        }
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        this.supplier.reset(confirmedDeals);
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan newPlan) {
        this.supplier.reset(confirmedDeals, newPlan);
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.iterator;
    }
}

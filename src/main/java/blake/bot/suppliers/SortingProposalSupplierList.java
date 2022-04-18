package blake.bot.suppliers;

import blake.bot.analyser.PlanCache;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.*;
import java.util.stream.Collectors;

public class SortingProposalSupplierList implements DealGenerator {
    public static final BasicDeal DUMMY_DEAL = new BasicDeal(Collections.emptyList(), Collections.emptyList());
    private final DealGenerator[] suppliers;
    private final Iterator<BasicDeal> myIterator;
    private final PlanCache planCache;
    private Map<DealGenerator, BasicDeal> planChoices;

    public SortingProposalSupplierList(PlanCache cache, DealGenerator... suppliers) {
        this.planCache = cache;
        this.suppliers = suppliers;
        myIterator = new BasicDealIterator(this::get);
        planChoices = Arrays.stream(suppliers).collect(Collectors.toMap(
                supplier -> supplier,
                supplier -> {
                    if (supplier.iterator().hasNext()) {
                        return supplier.iterator().next();
                    } else {
                        return DUMMY_DEAL;
                    }
                }
        ));
    }

    private BasicDeal get() {
        BasicDeal ret;

        List<Map.Entry<DealGenerator, BasicDeal>> list = new ArrayList<>(planChoices.entrySet());
        list.sort((o1, o2) -> planCache.getDealComparator().compare(o1.getValue(), o2.getValue()));

        final Map.Entry<DealGenerator, BasicDeal> bestChoice = list.get(0);
        ret = bestChoice.getValue();
        if (ret == DUMMY_DEAL) {
            return null;
        }

        final DealGenerator dealGenerator = bestChoice.getKey();
        planChoices.put(dealGenerator, dealGenerator.iterator().hasNext() ? dealGenerator.iterator().next() : DUMMY_DEAL);
        return ret;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals);
        }
        planChoices = Arrays.stream(suppliers).collect(Collectors.toMap(
                supplier -> supplier,
                supplier -> {
                    if (supplier.iterator().hasNext()) {
                        return supplier.iterator().next();
                    } else {
                        return DUMMY_DEAL;
                    }
                }
        ));
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan plan) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals, plan);
        }
        planChoices = Arrays.stream(suppliers).collect(Collectors.toMap(
                supplier -> supplier,
                supplier -> {
                    if (supplier.iterator().hasNext()) {
                        return supplier.iterator().next();
                    } else {
                        return DUMMY_DEAL;
                    }
                }
        ));
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return myIterator;
    }
}

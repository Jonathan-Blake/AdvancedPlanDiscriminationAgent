package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CyclingProposalSupplierList implements DealGenerator {

    private final DealGenerator[] suppliers;
    private final Iterator<BasicDeal> myIterator;
    private Iterator<BasicDeal> iter;
    private Iterator<DealGenerator> supplierIterator;
    private boolean dealFoundThisLoop;

    public CyclingProposalSupplierList(DealGenerator... suppliers) {
        this.suppliers = suppliers;
        this.iter = null;
        myIterator = new BasicDealIterator(this::get);
        supplierIterator = Arrays.stream(suppliers).iterator();
        dealFoundThisLoop = false;
    }

    private BasicDeal get() {
        BasicDeal ret;
        if (supplierIterator.hasNext()) {
            iter = supplierIterator.next().iterator();
            if (iter.hasNext()) {
                ret = iter.next();
                dealFoundThisLoop = true;
            } else {
                ret = get();
            }
        } else {
            if (dealFoundThisLoop) {
                supplierIterator = Arrays.stream(suppliers).iterator();
                dealFoundThisLoop = false;
                ret = get();
            } else {
                ret = null;
            }
        }
        return ret;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan plan) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals, plan);
        }
        supplierIterator = Arrays.stream(suppliers).iterator();
        dealFoundThisLoop = false;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals);
        }
        supplierIterator = Arrays.stream(suppliers).iterator();
        dealFoundThisLoop = false;
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.myIterator;
    }
}

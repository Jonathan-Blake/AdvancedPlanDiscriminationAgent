package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.tools.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ProposalSupplierList implements DealGenerator {
    private final DealGenerator[] suppliers;
    private Logger logger;

    private Iterator<BasicDeal> iter;
    private final Iterator<BasicDeal> myIterator;
    private Iterator<DealGenerator> supplierIterator;

    public ProposalSupplierList(DealGenerator... suppliers) {
        this.suppliers = suppliers;
        this.iter = null;
        myIterator = new BasicDealIterator(this::get);
        supplierIterator = Arrays.stream(suppliers).iterator();
    }

    public ProposalSupplierList(Logger logger, DealGenerator... suppliers) {
        this(suppliers);
        this.logger = logger;
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan plan) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals, plan);
        }
        supplierIterator = Arrays.stream(suppliers).iterator();
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        for (DealGenerator supplier : suppliers) {
            supplier.reset(confirmedDeals);
        }
        supplierIterator = Arrays.stream(suppliers).iterator();
    }

    private BasicDeal get() {
        BasicDeal ret;
        if (iter != null && iter.hasNext()) {
            ret = iter.next();
        } else if (supplierIterator.hasNext()) {
            iter = supplierIterator.next().iterator();
            ret = get();
        } else {
            ret = null;
        }
        if (this.logger != null) {
            this.logger.logln("Attempted to get proposal : " + ret);
        }
        return ret;
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.myIterator;
    }
}

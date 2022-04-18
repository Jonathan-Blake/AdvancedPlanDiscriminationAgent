package blake.bot.suppliers;

import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class EagerProposalSupplier implements DealGenerator {
    private final DealGenerator subSupplier;
    private Iterator<BasicDeal> iterator;

    public EagerProposalSupplier(DealGenerator dealGenerator) {
        this.subSupplier = dealGenerator;
        iterator = dealGenerator.stream().collect(Collectors.toList()).iterator();
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals) {
        this.subSupplier.reset(confirmedDeals);
        iterator = subSupplier.stream().collect(Collectors.toList()).iterator();
    }

    @Override
    public void reset(List<BasicDeal> confirmedDeals, Plan newPlan) {
        this.subSupplier.reset(confirmedDeals, newPlan);
        iterator = subSupplier.stream().collect(Collectors.toList()).iterator();
    }

    @Override
    public Iterator<BasicDeal> iterator() {
        return this.iterator;
    }
}

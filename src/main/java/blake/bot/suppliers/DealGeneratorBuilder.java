package blake.bot.suppliers;

import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.function.Predicate;

public class DealGeneratorBuilder {

    DealGenerator building;

    private DealGeneratorBuilder() {
    }

    public static DealGeneratorBuilder get() {
        return new DealGeneratorBuilder();
    }

    public DealGeneratorBuilder filter(Predicate<BasicDeal> filter) {
        building = new FilteredProposalSupplier(filter, building);
        return this;
    }

    public DealGeneratorBuilder eager() {
        building = new EagerProposalSupplier(building);
        return this;
    }
}

package blake.bot.suppliers;

import ddejonge.bandana.negoProtocol.BasicDeal;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class BasicDealIterator implements Iterator<BasicDeal> {
    private final Supplier<BasicDeal> supplier;
    private BasicDeal next = null;

    public BasicDealIterator(Supplier<BasicDeal> supplier) {
        this.supplier = supplier;
    }


    @Override
    public boolean hasNext() {
        if (next == null) {
            next = get();
        }
        return next != null;
    }

    @Override
    public BasicDeal next() {
        BasicDeal temp = null;
        if (hasNext()) {
            temp = this.next;
            this.next = null;
        }
        if (temp == null) {
            throw new NoSuchElementException();
        }
        return temp;
    }

    private BasicDeal get() {
        return this.supplier.get();
    }
}

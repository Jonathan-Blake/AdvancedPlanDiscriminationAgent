package blake.bot.utility;

import blake.bot.analyser.PlanInfo;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Phase;

import java.util.Objects;

public class DatedObject {

    private final Phase phase;
    private final int year;

    public DatedObject(OrderCommitment order) {
        this.phase = order.getPhase();
        this.year = order.getYear();
    }

    public DatedObject(DMZ dmz) {
        this.phase = dmz.getPhase();
        this.year = dmz.getYear();
    }

    public DatedObject(int year, Phase phase) {
        this.year = year;
        this.phase = phase;
    }

    public DatedObject(Game game) {
        this.phase = game.getPhase();
        this.year = game.getYear();
    }

    public DatedObject(PlanInfo planInfo) {
        this(planInfo.getGame());
    }

    public Phase getPhase() {
        return this.phase;
    }

    public int getYear() {
        return this.year;
    }

    @Override
    public String toString() {
        return String.format("DatedObject{phase=%s, year=%d}", phase, year);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DatedObject) {
            return this.equals((DatedObject) obj);
        }
        return super.equals(obj);
    }

    public boolean equals(DatedObject obj) {
        if (this == obj) {
            return true;
        } else {
            return this.phase == obj.phase && this.year == obj.year;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPhase(), getYear());
    }
}

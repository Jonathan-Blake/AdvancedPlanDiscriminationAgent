package blake.bot.utility;

import es.csic.iiia.fabregues.dip.board.Power;

import java.util.Objects;

public class HashedPower {
    private final Power power;

    public HashedPower(Power power) {
        if (power == null) throw new AssertionError();
        this.power = power;
    }

    public Power asPower() {
        return power;
    }

    public String asString() {
        return power.getName();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(power.getName());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof String) {
            return equals((String) other);
        } else if (other instanceof Power) {
            return equals((Power) other);
        } else if (other instanceof HashedPower) {
            return equals((HashedPower) other);
        } else {
            return false;
        }
    }

    public boolean equals(Power other) {
        return this.power.getName().equals(other.getName());
    }

    public boolean equals(HashedPower other) {
        return this.power.getName().equals(other.power.getName());
    }

    public boolean equals(String other) {
        return this.power.getName().equals(other);
    }
}

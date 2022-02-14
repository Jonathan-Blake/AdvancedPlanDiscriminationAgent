package blake.bot.utility;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RelationshipMatrix<T> {

    private final Map<HashedPower, Map<HashedPower, T>> matrix;
    private final HashedPower me;
    private final Predicate<T> allyPredicate;
    private final List<Power> nonNegotiators;

    public RelationshipMatrix(Power me, List<Power> nonNegotiators, Collection<Power> negotiators, T defaultRelationship, Predicate<T> allyPredicate) {
        this.me = new HashedPower(me);
        this.allyPredicate = allyPredicate;
        this.nonNegotiators = nonNegotiators;
        this.matrix = new HashMap<>();
        List<HashedPower> hashableNegotiators = negotiators.stream().map(HashedPower::new).collect(Collectors.toList());
        if (!hashableNegotiators.contains(this.me)) {
            hashableNegotiators.add(this.me);
        }
        for (HashedPower each : hashableNegotiators) {
            matrix.put(each, hashableNegotiators.stream().collect(Collectors.toMap(
                    power -> power,
                    power -> defaultRelationship
            )));
        }

    }

    public static RelationshipMatrix<Relationship> getDefaultMatrix(Power me, List<Power> negotiators, Game game) {
        return new RelationshipMatrix<>(me, Utility.Lists.createFilteredList(game.getNonDeadPowers(), negotiators), negotiators, Relationship.NEUTRAL, (relationship -> relationship == Relationship.ALLIED));
    }

    public boolean setRelationship(Power other, T value) {
        return setRelationship(this.me, new HashedPower(other), value);
    }

    private boolean setRelationship(HashedPower power, HashedPower other, T value) {
        if (nonNegotiators.stream().anyMatch(nonNegotiator -> power.equals(nonNegotiator) || other.equals(nonNegotiator))) {
            return false;
        }
        matrix.get(power).put(other, value);
        matrix.get(other).put(power, value);
        return true;
    }

    public List<Power> getAllies() {
        return getAllies(this.me);
    }

    private List<Power> getAllies(HashedPower power) {
        return matrix.get(power).entrySet().stream()
                .filter(stringTEntry -> !allyPredicate.test(stringTEntry.getValue()))
                .map(stringTEntry -> stringTEntry.getKey().asPower())
                .collect(Collectors.toList());
    }
}

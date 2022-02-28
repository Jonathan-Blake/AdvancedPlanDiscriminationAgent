package blake.bot.utility;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationshipMatrix<T> {

    private final Map<HashedPower, Map<HashedPower, T>> matrix;
    private final HashedPower me;
    private final Collection<HashedPower> nonNegotiators;
    private final Function<T, Relationship> relationshipFunction;
    private final T defaultRelationship;

    public RelationshipMatrix(Power me, Collection<Power> nonNegotiators, Collection<Power> negotiators, T defaultRelationship, Function<T, Relationship> relationshipFunction) {
        this(new HashedPower(me),
                nonNegotiators.stream().map(HashedPower::new).collect(Collectors.toList()),
                negotiators.stream().map(HashedPower::new).collect(Collectors.toList()),
                defaultRelationship,
                relationshipFunction
        );
    }

    public RelationshipMatrix(HashedPower me, Collection<HashedPower> nonNegotiators, Collection<HashedPower> negotiators, T defaultRelationship, Function<T, Relationship> relationshipFunction) {
        this.me = me;
        this.relationshipFunction = relationshipFunction;
        this.nonNegotiators = nonNegotiators;
        this.matrix = new HashMap<>();
        this.defaultRelationship = defaultRelationship;
        if (!negotiators.contains(this.me)) {
            negotiators.add(this.me);
        }
        for (HashedPower each : negotiators) {
            matrix.put(each, negotiators.stream().collect(Collectors.toMap(
                    power -> power,
                    power -> defaultRelationship
            )));
        }
    }

    public static RelationshipMatrix<Relationship> getDefaultMatrix(Power me, List<Power> negotiators, Game game) {
        return new RelationshipMatrix<>(me, Utility.Lists.createFilteredList(game.getNonDeadPowers(), negotiators), negotiators, Relationship.NEUTRAL, (Function.identity()));
    }

    public static RelationshipMatrix<Relationship> getDefaultMatrix(Power me, Collection<Power> negotiators, Collection<Power> nonNegotiators) {
        return new RelationshipMatrix<>(me, nonNegotiators, negotiators, Relationship.NEUTRAL, (Function.identity()));
    }

    public static RelationshipMatrix<Double> getDoubleMatrix(Power me, List<Power> negotiators, Game game, double initialValue, Function<Double, Relationship> valueToRelationship) {
        return new RelationshipMatrix<>(me, Utility.Lists.createFilteredList(game.getNonDeadPowers(), negotiators), negotiators, initialValue, valueToRelationship);
    }

    public RelationshipMatrix<Relationship> convert() {
        RelationshipMatrix<Relationship> ret = new RelationshipMatrix<>(this.me, this.nonNegotiators, new ArrayList<>(this.matrix.keySet()), this.relationshipFunction.apply(this.defaultRelationship), (rel -> rel));
        this.matrix.forEach(
                (outerKey, outerValue) -> outerValue.forEach(
                        (innerKey, innerValue) -> ret.setRelationship(outerKey, innerKey, this.relationshipFunction.apply(innerValue)))
        );
        return ret;
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

    public List<Power> getAllies(Power power) {
        return getAllies(new HashedPower(power));
    }

    public List<Power> getAllies(HashedPower power) {
        if (this.defaultRelationship instanceof Relationship) {
            return matrix.get(power).entrySet().stream()
                    .filter(entrySet -> entrySet.getValue() == Relationship.ALLIED)
                    .map(stringTEntry -> stringTEntry.getKey().asPower())
                    .collect(Collectors.toList());
        } else {
            return convert().getAllies(power);
        }
    }

    public Optional<T> getRelationship(Power other) {
        return getRelationship(this.me, other);
    }

    private Optional<T> getRelationship(HashedPower me, Power other) {
        return this.matrix.get(me).entrySet().stream()
                .filter(entrySet -> entrySet.getKey().asPower().equals(other))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}

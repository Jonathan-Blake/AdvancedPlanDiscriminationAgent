package blake.bot.utility;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;

import java.util.*;

public class JavaDumbbot {
    private static final int[] sprProximityWeights = {100, 1000, 30, 10, 6, 5, 4, 3, 2, 1};
    private static final int[] fallProximityWeights = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};
    private static final int[] buildProximityWeights = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};
    private static final int[] removeProximityWeights = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};

    private static final int PROXIMITY_DEPTH = 10;
    final Game game;
    final Power me;
    private HashMap<Province, Float> defenseValue;
    private HashMap<Province, Integer> strengthValue;
    private HashMap<Province, Integer> competitionValue;
    private HashMap<Region, Float[]> proximity;
    private HashMap<Region, Integer> destinationValue;
    private boolean calculated;

    public JavaDumbbot(Game game, Power me, boolean lazyCalculation) {
        this.game = game;
        this.me = me;
        this.calculated = false;
        if (!lazyCalculation) {
            calculateFactors();
            initStrCompValues();
            calculateDestinationValue();
        }
    }

    public Map<Region, Integer> getDestinationValue() {
        if (!this.calculated) {
            calculateFactors();
            initStrCompValues();
            calculateDestinationValue();
        }
        return destinationValue;
    }

    private float getSize(final Power power) {
        if (power == null) {
            return 0.0F;
        }
        final float ownedSCs = power.getOwnedSCs().size();
        return ownedSCs * ownedSCs * 1.0F +
                ownedSCs * 4.0F + 16.0F;
    }

    protected void calculateDestinationValue() {
        switch (this.game.getPhase()) {
            case SPR:
            case SUM:
                calculateDestinationValue(fallProximityWeights, 1000, 1000);
                break;
            case AUT:
            case FAL:
                calculateDestinationValue(sprProximityWeights, 1000, 1000);
                break;
            case WIN:
                calculateWINDestinationValue(
                        this.me.getOwnedSCs().size() > this.me.getControlledRegions().size() ? removeProximityWeights : buildProximityWeights,
                        1000);
                break;
        }
    }

    private void calculateDestinationValue(int[] proximityWeights, int strengthWeight, int competitionWeight) {
        this.destinationValue = new HashMap<>(this.game.getRegions().size());
        for (Region region : this.game.getRegions()) {
            int destWeight = 0;
            for (int i = 0; i < 10; i++) {
                destWeight = (int) (destWeight + (this.proximity.get(region))[i] * proximityWeights[i]);
            }
            destWeight = destWeight + strengthWeight * this.strengthValue.get(region.getProvince());
            destWeight = destWeight - competitionWeight * this.competitionValue.get(region.getProvince());
            this.destinationValue.put(region, destWeight);
        }
        this.calculated = true;
//        List<Region> regions = this.game.getRegions();
//        regions.sort(new DestValueComparator(this.destinationValue));
    }

    private void calculateWINDestinationValue(int[] proximityWeight, int defenseWeight) {
        this.destinationValue = new HashMap<>(this.game.getRegions().size());
        for (Region region : this.game.getRegions()) {
            int destWeight = 0;
            int proxCount = 0;
            while (proxCount < PROXIMITY_DEPTH) {
                destWeight = (int) (destWeight + (this.proximity.get(region))[proxCount] * proximityWeight[proxCount]);
                proxCount++;
            }
            destWeight = (int) (destWeight + defenseWeight * this.defenseValue.get(region.getProvince()));
            this.destinationValue.put(region, destWeight);
        }
    }

    private Power getOwner(Province province) {
        for (Power power : this.game.getPowers()) {
            if (power.getOwnedSCs().contains(province)) {
                return power;
            }
        }
        return null;
    }

    public float calcDefVal(Province province) {
        float maxPower = 0.0F;
        List<Region> adjacentRegions = new ArrayList<>();
        List<Power> neighborPowers = new ArrayList<>();
        for (Region region : province.getRegions()) {
            adjacentRegions.addAll(region.getAdjacentRegions());
        }
        for (Region region : adjacentRegions) {
            neighborPowers.add(this.game.getController(region));
        }
        for (Power power : neighborPowers) {
            if ((power != null) && (!power.equals(this.me)) && (getSize(power) > maxPower)) {
                maxPower = getSize(power);
            }
        }
        return maxPower;
    }

    public void calculateFactors() {
        int proximityAttackWeight = 0;
        int proximityDefWeight = 0;
        switch (this.game.getPhase()) {
            case SPR:
            case SUM:
                proximityAttackWeight = 600;
                proximityDefWeight = 400;
                break;
            case AUT:
            case FAL:
            case WIN:
                proximityAttackWeight = 700;
                proximityDefWeight = 300;
                break;
        }

        this.defenseValue = new HashMap<>();
        HashMap<Province, Float> attackValue = new HashMap<>();
        this.game.getProvinces().forEach(province -> setInitialAttackDefenseValues(attackValue, province));

        calculateNZeroProximities(proximityAttackWeight, proximityDefWeight, attackValue);

        int n = 1;
        while (n < PROXIMITY_DEPTH) {
            for (Province province : this.game.getProvinces()) {
                for (Region region : province.getRegions()) {
                    calculateNDeepProximities(n, region);
                    this.proximity.get(region)[n] = this.proximity.get(region)[n] / 5.0F;
                }
            }
            n++;
        }
    }


    private void setInitialAttackDefenseValues(HashMap<Province, Float> attackValue, Province province) {
        if (province.isSC()) {
            if (this.me.getOwnedSCs().contains(province)) {
                this.defenseValue.put(province, calcDefVal(province));
                attackValue.put(province, 0.0F);
            } else {
                attackValue.put(province, getSize(getOwner(province)));
                this.defenseValue.put(province, 0.0F);
            }
        } else {
            attackValue.put(province, 0.0F);
            this.defenseValue.put(province, 0.0F);
        }
    }

    private void calculateNZeroProximities(int proximityAttackWeight, int proximityDefenseWeight, HashMap<Province, Float> attackValue) {
        this.proximity = new HashMap<>();
        for (Province province : this.game.getProvinces()) {
            for (Region region : province.getRegions()) {
                Float[] proximities = new Float[10];
                proximities[0] = attackValue.get(province) * proximityAttackWeight + this.defenseValue.get(province) * proximityDefenseWeight;
                this.proximity.put(region, proximities);
            }
        }
    }

    private void calculateNDeepProximities(int n, Region region) {
        Float[] proximities = this.proximity.get(region);
        proximities[n] = proximities[n - 1];

        Region multipleCoasts = null;
        for (Region adjRegion : region.getAdjacentRegions()) {
            if ((adjRegion.getName().substring(4).compareTo("CS") == 0) && (multipleCoasts != null)) {
                if ((this.proximity.get(adjRegion))[(n - 1)] > (this.proximity.get(multipleCoasts))[(n - 1)]) {
                    (this.proximity.get(region))[n] = (this.proximity.get(region))[n] - (this.proximity.get(multipleCoasts))[(n - 1)] + (this.proximity.get(adjRegion))[(n - 1)];
                }
            } else {
                (this.proximity.get(region))[n] = (this.proximity.get(region))[n] + (this.proximity.get(adjRegion))[(n - 1)];
                if (adjRegion.getName().substring(4).compareTo("CS") == 0) {
                    multipleCoasts = adjRegion;
                }
            }
        }
    }

    public void initStrCompValues() {
        this.strengthValue = new HashMap<>();
        this.competitionValue = new HashMap<>();
        for (Province province : this.game.getProvinces()) {
            Map<Power, Integer> adjUnitCount = countAdjacentUnits(province);

            for (Power power : this.game.getPowers()) {
                if (power.equals(this.me)) {
                    this.strengthValue.put(province, adjUnitCount.get(this.me));
//                } else if (!this.competitionValue.containsKey(province)) {
//                    this.competitionValue.put(province, adjUnitCount.get(power));
//                } else if (adjUnitCount.get(power) > this.competitionValue.get(province)) {
//                    this.competitionValue.put(province, adjUnitCount.get(power));
//                }
                } else {
                    this.competitionValue.merge(
                            province,
                            adjUnitCount.get(power),
                            (oldValue, newValue) -> newValue > oldValue ? newValue : oldValue
                    );
                }
            }
        }

//        for (Province province : this.game.getProvinces()) {
//            if (!this.competitionValue.containsKey(province)) {
//                this.competitionValue.put(province, 0);
//            }
//            if (!this.strengthValue.containsKey(province)) {
//                this.strengthValue.put(province, 0);
//            }
//        }
        this.game.getProvinces().forEach(province -> {
            this.competitionValue.putIfAbsent(province, 0);
            this.strengthValue.putIfAbsent(province, 0);
        });
    }

    private Map<Power, Integer> countAdjacentUnits(Province province) {
        Map<Power, Integer> adjUnitCount = new HashMap<>();
        for (Power power : this.game.getPowers()) {
            int count = 0;
            for (Region unit : power.getControlledRegions()) {
                for (Region region : province.getRegions()) {
                    if (region.getAdjacentRegions().contains(unit)) {
                        count++;
                        break;
                    }
                }
            }
            adjUnitCount.put(power, count);
        }
        return adjUnitCount;
    }

    static class DestValueComparator implements Comparator<Region> {
        private final Map<Region, Integer> destinationValue;

        public DestValueComparator(Map<Region, Integer> destValue) {
            this.destinationValue = destValue;
        }

        public int compare(Region region1, Region region2) {
//            return -(this.destinationValue.get(region1)).compareTo(this.destinationValue.get(region2));
            return -Integer.compare(this.destinationValue.get(region1), this.destinationValue.get(region2));
        }
    }
}

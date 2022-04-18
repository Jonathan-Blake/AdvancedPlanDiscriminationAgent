package blake.bot.analyser;

import blake.bot.utility.HashedPower;
import ddejonge.bandana.negoProtocol.BasicDeal;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlanInfo {
    private final Game game;
    private final HashedPower me;
    private final List<BasicDeal> commitments;
    private final BasicDeal deal;
    private final List<HashedPower> allies;
    private String dealId;

    public PlanInfo(Game game, HashedPower me, List<BasicDeal> commitments, BasicDeal deal) {
        this.game = game;
        this.me = me;
        this.deal = deal;
        this.commitments = commitments;
        this.allies = Collections.emptyList();
    }

    public Game getGame() {
        return this.game;
    }

    public HashedPower getMe() {
        return this.me;
    }

    public List<HashedPower> getAllies() {
        return allies;
    }

    public List<BasicDeal> getCommitments() {
        return commitments;
    }

    public BasicDeal getDeal() {
        return this.deal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanInfo planInfo = (PlanInfo) o;
        return getGame().equals(planInfo.getGame())
                && getMe().equals(planInfo.getMe())
                && Objects.equals(getAllies(), planInfo.getAllies())
                && Objects.equals(getCommitments(), planInfo.getCommitments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGame(), getMe(), getAllies(), getCommitments());
    }

    public List<Power> getAlliesAsPower() {
        return this.getAllies().stream().map(HashedPower::asPower).collect(Collectors.toList());
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }
}

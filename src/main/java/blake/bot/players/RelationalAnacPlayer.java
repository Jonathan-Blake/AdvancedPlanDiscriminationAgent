package blake.bot.players;

import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.*;
import ddejonge.bandana.tools.Logger;
import ddejonge.bandana.tools.Utilities;
import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.*;
import es.csic.iiia.fabregues.dip.comm.CommException;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.DSBOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.RTOOrder;

import java.io.File;
import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class RelationalAnacPlayer extends Player {
    public static final int DEFAULT_FINAL_YEAR = 2000;
    public static final int NEGOTIATION_LENGTH = 3000;
    final DBraneTactics dbraneTactics = new DBraneTactics();
    final Logger logger = new Logger();
    private final Random random = new Random();
    RelationalNegotiator anacNegotiator;
    DiplomacyNegoClient negoClient;
    int gameServerPort;
    int negoServerPort;
    int finalYear;
    ArrayList<BasicDeal> confirmedDeals = new ArrayList<>();
    private IComm communicator;

    RelationalAnacPlayer() {
        //Exists For Testing Purposes
    }

    public RelationalAnacPlayer(RelationalNegotiator anacNegotiator, String name, String logPath, int finalYear, int gameServerPort, int negoServerPort) {
        super(logPath);
        this.name = name;
        this.finalYear = finalYear;
        this.logPath = logPath;
        this.anacNegotiator = anacNegotiator;
        this.gameServerPort = gameServerPort;
        this.negoServerPort = negoServerPort;
        this.negoClient = new DiplomacyNegoClient(this, negoServerPort);
    }

    public void run() {
        File logFolder = new File(this.logPath);
        logFolder.mkdirs();

        try {
            InetAddress dipServerIp = InetAddress.getByName("localhost");
            this.communicator = new DaideComm(dipServerIp, this.gameServerPort, this.name);
            this.start(this.communicator);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public void init() {
        this.anacNegotiator.setMe(this.me);
        this.logger.enable(this.logPath, this.me.getName() + ".log");
        this.logger.logln(this.name + " playing as " + this.me.getName(), true);
        this.logger.writeToFile();
        this.negoClient.connect();
        this.negoClient.waitTillReady();
    }

    public void start() {
        this.anacNegotiator.start();
    }

    public List<Order> play() {
        this.anacNegotiator.setGame(this.game);
        ArrayList<Order> myOrders = new ArrayList<>();
        this.confirmedDeals.clear();

        for (Deal confirmedDeal : this.negoClient.getConfirmedDeals()) {
            if (Utilities.testValidity(this.game, (BasicDeal) confirmedDeal) == null) {
                this.confirmedDeals.add(((BasicDeal) confirmedDeal));
            }
        }

        if (this.game.getPhase() != Phase.SPR && this.game.getPhase() != Phase.FAL) {
            List<Power> myAllies = this.anacNegotiator.getRelationships().getAllies();
            return this.game.getPhase() != Phase.SUM && this.game.getPhase() != Phase.AUT ? this.dbraneTactics.getWinterOrders(this.game, this.me, myAllies) : this.generateRandomRetreats();
        } else {
//            Iterator var6;
            try {
                long negotiationDeadline = System.currentTimeMillis() + 3000L;
                this.anacNegotiator.negotiate(negotiationDeadline);
                List<Power> myAllies = this.anacNegotiator.getRelationships().getAllies();
                this.confirmedDeals.clear();
//                var6 = this.negoClient.getConfirmedDeals().iterator();

//                while(var6.hasNext()) {
                for (BasicDeal confirmedDeal : this.negoClient.getConfirmedDeals().stream().map(BasicDeal.class::cast).collect(Collectors.toList())) {
//                    BasicDeal confirmedDeal = (BasicDeal)var6.next();


                    if (Utilities.testValidity(this.game, confirmedDeal) == null) {
                        this.confirmedDeals.add(confirmedDeal);
                    }
                }

                this.logger.logln();
                this.logger.logln(this.game.getYear() + " " + this.game.getPhase());
                this.logger.logln("Confirmed Deals: " + this.confirmedDeals);
                String report = Utilities.testConsistency(this.game, this.confirmedDeals);
                if (report != null) {
                    this.logger.logln("ERROR! confirmed deals are inconsistent: " + report);
                } else {
                    Plan plan = this.dbraneTactics.determineBestPlan(this.game, this.me, this.confirmedDeals, myAllies);
                    if (plan == null) {
                        this.logger.logln("ERROR! plan == null!");
                    } else {
                        myOrders.addAll(plan.getMyOrders());
                    }
                }
            } catch (Exception var9) {
                var9.printStackTrace();
            }

            List<Order> committedOrders = new ArrayList<>();
            List<DMZ> demilitarizedZones = new ArrayList<>();

            for (BasicDeal deal : this.confirmedDeals) {

                for (DMZ dmz : deal.getDemilitarizedZones()) {
                    if (dmz.getPhase().equals(this.game.getPhase()) && dmz.getYear() == this.game.getYear() && dmz.getPowers().contains(this.me)) {
                        demilitarizedZones.add(dmz);
                    }
                }

                for (OrderCommitment orderCommitment : deal.getOrderCommitments()) {
                    if (orderCommitment.getPhase().equals(this.game.getPhase()) && orderCommitment.getYear() == this.game.getYear() && orderCommitment.getOrder().getPower().equals(this.me)) {
                        committedOrders.add(orderCommitment.getOrder());
                    }
                }
            }

            myOrders = Utilities.addHoldOrders(this.me, myOrders);
            this.logger.logln("Commitments to obey this turn: " + committedOrders + " " + demilitarizedZones);
            this.logger.logln("Orders submitted this turn: " + myOrders);
            this.logger.writeToFile();
            return myOrders;
        }
    }

    private List<Order> generateRandomRetreats() {
        List<Order> orders = new ArrayList<>(this.game.getDislodgedRegions().size());
        HashMap<Region, Dislodgement> units = this.game.getDislodgedRegions();
        List<Region> dislodgedUnits = this.game.getDislodgedRegions(this.me);

        for (Region region : dislodgedUnits) {
            Dislodgement dislodgement = units.get(region);
            List<Region> dest = new ArrayList<>(dislodgement.getRetreateTo());
            if (dest.isEmpty()) {
                orders.add(new DSBOrder(region, this.me));
            } else {
                int randomInt = this.random.nextInt(dest.size());
                orders.add(new RTOOrder(region, this.me, dest.get(randomInt)));
            }
        }

        return orders;
    }

    public void receivedOrder(Order arg0) {
        this.anacNegotiator.receivedOrder(arg0);
    }

    @Override
    public void phaseEnd(GameState gameState) {
        if (this.game.getYear() == this.finalYear && this.game.getPhase() == Phase.FAL || this.game.getYear() > this.finalYear) {
            this.proposeDraw();
        }

    }

    void proposeDraw() {
        try {
            this.communicator.sendMessage(new String[]{"DRW"});
        } catch (CommException var2) {
            var2.printStackTrace();
        }

    }

    @Override
    public void handleSMR(String[] message) {
        this.logger.writeToFile();
        this.communicator.stop();
        this.negoClient.closeConnection();
        this.exit();
    }

    @Override
    public void submissionError(String[] message) {
        if (message.length < 2) {
            logger.logln("submissionError() " + Arrays.toString(message));
        } else {
            String illegalOrder = "";

            for (int i = 2; i < message.length - 4; ++i) {
                illegalOrder = String.format("%s%s ", illegalOrder, message[i]);
            }

            logger.logln(String.format("Illegal order submitted: %s%n", illegalOrder));
            String errorType = message[message.length - 2];
            switch (errorType.hashCode()) {
                case 67044:
                    if (errorType.equals("CST")) {
                        logger.logln("Reason: No coast specified for fleet build in StP, or an attempt to build a fleet inland, or an army at sea.");
                        return;
                    }
                    break;
                case 68949:
                    if (errorType.equals("ESC")) {
                        logger.logln("Reason: Not an empty supply centre");
                        return;
                    }
                    break;
                case 69367:
                    if (errorType.equals("FAR")) {
                        logger.logln("Reason: Unit is trying to move to a non-adjacent region, or is trying to support a move to a non-adjacent region.");
                        return;
                    }
                    break;
                case 71832:
                    if (errorType.equals("HSC")) {
                        logger.logln("Reason: Not a home supply centre");
                        return;
                    }
                    break;
                case 77056:
                    if (errorType.equals("NAS")) {
                        logger.logln("Reason: Not at sea (for a convoying fleet)");
                        return;
                    }
                    break;
                case 77411:
                    if (errorType.equals("NMB")) {
                        logger.logln("Reason: No more builds allowed");
                        return;
                    }
                    break;
                case 77427:
                    if (errorType.equals("NMR")) {
                        logger.logln("Reason: No more removals allowed");
                        return;
                    }
                    break;
                case 77578:
                    if (errorType.equals("NRN")) {
                        logger.logln("Reason: No retreat needed for this unit");
                        return;
                    }
                    break;
                case 77583:
                    if (errorType.equals("NRS")) {
                        logger.logln("Reason: Not the right season");
                        return;
                    }
                    break;
                case 77596:
                    if (errorType.equals("NSA")) {
                        logger.logln("Reason: No such army (for unit being ordered to CTO or for unit being CVYed)");
                        return;
                    }
                    break;
                case 77598:
                    if (errorType.equals("NSC")) {
                        logger.logln("Reason: Not a supply centre");
                        return;
                    }
                    break;
                case 77601:
                    if (errorType.equals("NSF")) {
                        logger.logln("Reason: No such fleet (in VIA section of CTO or the unit performing a CVY)");
                        return;
                    }
                    break;
                case 77611:
                    if (errorType.equals("NSP")) {
                        logger.logln("Reason: No such province.");
                        return;
                    }
                    break;
                case 77616:
                    if (errorType.equals("NSU")) {
                        logger.logln("Reason: No such unit.");
                        return;
                    }
                    break;
                case 77706:
                    if (errorType.equals("NVR")) {
                        logger.logln("Reason: Not a valid retreat space");
                        return;
                    }
                    break;
                case 77802:
                    if (errorType.equals("NYU")) {
                        logger.logln("Reason: Not your unit");
                        return;
                    }
                    break;
                case 88169:
                    if (errorType.equals("YSC")) {
                        logger.logln("Reason: Not your supply centre");
                        return;
                    }
                    break;
                default:
                    logger.logln("submissionError() Received error message of hash code: " + errorType.hashCode());
            }
            logger.logln("submissionError() Received error message of unknown type: " + Arrays.toString(message));

        }
    }
}

package negotiator.groupn;

import java.util.List;
import java.util.Map;
import java.util.Random;

import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.Domain;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class GroupZCK extends AbstractNegotiationParty {

    private final Random rand;
    private Bid currentBid;

    /**
     * Please keep this constructor. This is called by genius.
     *
     * @param utilitySpace Your utility space.
     * @param deadlines The deadlines set for this negotiation.
     * @param timeline Value counting from 0 (start) to 1 (end).
     * @param randomSeed If you use any randomization, use this seed for it.
     */
    public GroupZCK(UtilitySpace utilitySpace,
                    Map<DeadlineType, Object> deadlines,
                    Timeline timeline,
                    long randomSeed) {
        // Make sure that this constructor calls it's parent.
        super(utilitySpace, deadlines, timeline, randomSeed);
        this.rand = new Random(randomSeed);
        for(Issue i: this.getUtilitySpace().getDomain().getIssues()) {
            IssueDiscrete di = (IssueDiscrete)i;
            System.out.format(" %s %d\n", di, di.getNumberOfValues());
        }
    }

    /**
     * Each round this method gets called and ask you to accept or offer. The first party in
     * the first round is a bit different, it can only propose an offer.
     *
     * @param validActions Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */

    double Bj = 0;
    double Uj = 0;
    @Override
    public Action chooseAction(List<Class> validActions) {
        // with 50% chance, counter offer
        // if we are the first party, also offer.
        System.out.println(this.utilitySpace.getDiscountFactor());
        Uj = Bj*Bj;
        Bj = 1./2 + Bj*Bj;
        try {
            if(currentBid != null && utilitySpace.getUtility(currentBid) >= Uj) {
                return new Accept();
            }
            else {
                return new Offer(this.utilitySpace.getMaxUtilityBid());
//                return new Offer(this.generateRandomBid());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

//		if (!validActions.contains(Accept.class) || timeline.getCurrentTime()<timeline.getTotalTime()-0.1) {
//            try {
//                return new Offer(this.utilitySpace.getMaxUtilityBid());
//            } catch (Exception e) {
//                e.printStackTrace();
//                return null;
//            }
//        }
//		else {
//			return new Accept();
//		}
    }


    /**
     * All offers proposed by the other parties will be received as a message.
     * You can use this information to your advantage, for example to predict their utility.
     *
     * @param sender The party that did the action.
     * @param action The action that party did.
     */
    @Override
    public void receiveMessage(Object sender, Action action) {
        super.receiveMessage(sender, action);
        currentBid = Action.getBidFromAction(action);
        // Here you can listen to other parties' messages
    }

}

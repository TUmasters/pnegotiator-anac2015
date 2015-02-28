package negotiator.groupn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import negotiator.DeadlineType;
import negotiator.Domain;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;
import negotiator.issue.Objective;
import negotiator.Bid;
import negotiator.issue.Value;


/**
 * This is your negotiation party.
 */
public class GroupZCK2 extends AbstractNegotiationParty {

    // Parameters of the negotiation
    private UtilitySpace utilitySpace;
    private Timeline timeline;

    // Other private fields
    private Domain domain;
    private ArrayList<Objective> objectives;
    private Bid currentBid = null;
    private Bid lastBid = null;
    private Bid nextBid = null;
    private ArrayList<HashMap<Value, Double>> normalizedEvaluations;
    private double issueConcessionFactor;
    private double utilityConcessionFactor;

    /**
     * Please keep this constructor. This is called by genius.
     *
     * @param utilitySpace Your utility space.
     * @param deadlines The deadlines set for this negotiation.
     * @param timeline Value counting from 0 (start) to 1 (end).
     * @param randomSeed If you use any randomization, use this seed for it.
     */
    public GroupZCK2(UtilitySpace utilitySpace,
                     Map<DeadlineType, Object> deadlines,
                     Timeline timeline,
                     long randomSeed) {
        // Make sure that this constructor calls it's parent.
        super(utilitySpace, deadlines, timeline, randomSeed);

        // Set agent's game parameters
        this.utilitySpace = utilitySpace;
        this.timeline = timeline;

        // Initialize private fields
        domain = utilitySpace.getDomain();
        objectives = domain.getObjectives();
        normalizedEvaluations = new ArrayList<HashMap<Value, Double>>();
        normalizedEvaluations.add(0, null); // to align with 1-based data structures inherited by agent

        // Print stuff for the benefit of our understanding
        System.out.print(utilitySpace.getNrOfEvaluators());
        System.out.print("Utility function " );
        for(int i = 1; i < objectives.size(); ++i)
        {
            System.out.print(utilitySpace.getEvaluator(i));
        }
        System.out.println("Discount factor: " + utilitySpace.getDiscountFactor());

        for(int i = 1; i < objectives.size(); ++i)
        {
            System.out.println("(Name, Number, Type, Weight): (" + objectives.get(i).getName() + ", " + objectives.get(i).getType() + ", "+ utilitySpace.getWeight(i) + ")");
        }
    }

    /**
     * Each round this method gets called and ask you to accept or offer. The first party in
     * the first round is a bit different, it can only propose an offer.
     *
     * @param validActions Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class> validActions) {

        // Decide whether to accept current bid or make a new offer.
        // Right now the agent is not implementing any interesting logic and will accept if the offered utility
        //    is within +/- .01 times the best attainable utility.... So this needs work.
        try {
            if(currentBid != null && utilitySpace.getMaxUtilityBid() != null) {
                if(!(computeUtility(utilitySpace.getMaxUtilityBid()) < (computeUtility(currentBid)) * .01)
                        && !(computeUtility(utilitySpace.getMaxUtilityBid()) > (computeUtility(currentBid)) * .01)) {
                    return new Accept();
                }
            }
        } catch (Exception e) { }

        // Determine the next bid
        try {
            // If this is the first round, initialize things.
            if(currentBid == null) {
                System.out.println("First round, current bid is null");
                // Initialize nextBid to the bid that gives max utility (getMaxUtilityBid() given by GENIUS framework)
                nextBid = utilitySpace.getMaxUtilityBid();
                System.out.println("Next bid set to max:\n" + utilitySpace.getMaxUtilityBid().toString());

                // Set up data structure that holds all issue options and their individual evaluations
                for(int i = 1; i < objectives.size(); ++i) {
                    HashMap<Value, Double> objHash = new HashMap<Value, Double>();
                    objHash.put(utilitySpace.getMaxUtilityBid().getValue(i), utilitySpace.getEvaluation(i, utilitySpace.getMaxUtilityBid()) * utilitySpace.getWeight(i));
                    objHash.put(utilitySpace.getMinUtilityBid().getValue(i), utilitySpace.getEvaluation(i, utilitySpace.getMinUtilityBid()) * utilitySpace.getWeight(i));
                    normalizedEvaluations.add(i, objHash);
                }

                // Print out min & max utilities. Max should be 1...
                System.out.println("Max Utility: " + computeUtility(utilitySpace.getMaxUtilityBid()));
                System.out.println("Min Utility: " + computeUtility(utilitySpace.getMinUtilityBid()));

                // Print out all issues & bid evaluations... At first we only have what's given by getMinUtilityBid() and getMaxUtilityBid()...?????
                // I feel like we should have access to all the options and what they yield for us, but I can't find where we can get that.
                for(int i = 1; i < normalizedEvaluations.size(); ++i) {
                    for(Map.Entry<Value, Double> entry : normalizedEvaluations.get(i).entrySet()) {
                        System.out.println(i + ". " + objectives.get(i).getName() + ": " + entry.getKey() + " = " + entry.getValue());
                    }
                }
            }
            else
            {
                // Adjust "strategy" (e.g. by what percent is this agent willing to concede...) based on current time
                // (Gets more "accepting" the closer we are to the end of the negotiation)
                System.out.println("Time: " + timeline.getTime());
                setConcessionFactor(timeline.getTime());
                System.out.println("Discount factor: " + utilitySpace.getDiscountFactor());
                // Decide what the next bid should be
                decideNextBid();
            }
        } catch (Exception e) { }

        // Print the total utility of the bid we are making and submit it
        lastBid = nextBid;
        System.out.println("Proposing bid with utility: " + computeUtility(nextBid));
        return new Offer(nextBid);
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
        // Here you can listen to other parties' messages
        if(Action.getBidFromAction(action) != null) {
            currentBid = Action.getBidFromAction(action);
        }
    }

    // Given a Bid, compute our utility
    // Un-comment the code in here to print the individual evaluations of each issue in the utility function
    private double computeUtility(Bid bid) {
        double utility = 0.0;
        //String bidStr = "";
        for(int i = 1; i < objectives.size(); ++i) {
            try {
                utility += utilitySpace.getWeight(i) * utilitySpace.getEvaluation(i, bid);
                //bidStr += "(" + objectives.get(i).getName() + ", " + utilitySpace.getEvaluation(i, bid) + ")   ";
            } catch (Exception e) { }
        }
        //System.out.println("Bid String: " + bidStr);
        //System.out.println("Utility Yield: " + utility);
        return utility;
    }

    // A dumb function that just sets a scalar concession factor based on the current time
    private void setConcessionFactor(double time) {
        if(time < .3) {
            issueConcessionFactor = .95;
            utilityConcessionFactor = .95;
        }
        else if(time >= .3 && time < .5) {
            issueConcessionFactor = .85;
            utilityConcessionFactor = .9;
        }
        else if(time >= .5 && time < .8) {
            issueConcessionFactor = .8;
            utilityConcessionFactor = .8;
        }
        else if(time >= .8 && time < .93) {
            issueConcessionFactor = .75;
            utilityConcessionFactor = .75;
        }
        else if(time >= .93) {
            issueConcessionFactor = .7;
            utilityConcessionFactor = .7;
        }
    }

    // Logic to decide the next bid
    private void decideNextBid() {
        // Iterate through issues individually
        for(int i = 1; i < objectives.size(); ++i) {
            // Cache evaluation hashmap for this issue/objective
            HashMap<Value, Double> tempMap = normalizedEvaluations.get(i);

            // We may need to add previously unknown elements to our data structure
            // (I still don't understand why we don't know all of these at the very beginning????)
            try {
                if(!tempMap.containsKey(currentBid.getValue(i))) {
                    tempMap.put(currentBid.getValue(i), utilitySpace.getEvaluation(i, currentBid) * utilitySpace.getWeight(i));
                }
            } catch (Exception e) { System.out.println("ERROR: 1 :( "); }

            // Try to determine what to offer next....
            try {
                // If we didn't get offered our max for this issue...
                if(!(currentBid.getValue(i).equals(utilitySpace.getMaxUtilityBid().getValue(i)))) {
                    // See if there's another bid that we can offer that's between their offer and our offer
                    //    that doesn't compromise TOO much of our utility (based on concession factors)
                    for(Map.Entry<Value, Double> entry : tempMap.entrySet()) {
                        if((entry.getValue() >= tempMap.get(currentBid.getValue(i)))
                                && (entry.getValue() < tempMap.get(utilitySpace.getMaxUtilityBid().getValue(i)))
                                && (entry.getValue() >= (issueConcessionFactor * tempMap.get(utilitySpace.getMaxUtilityBid().getValue(i))))) {
                            nextBid.setValue(i, entry.getKey());
                            if(computeUtility(nextBid) >= (utilityConcessionFactor * computeUtility(lastBid))) {
                                nextBid.setValue(i, lastBid.getValue(i));
                            }
                        }
                    }
                }
            } catch (Exception e) { System.out.println("ERROR: 2 :( "); }
        }
    }

}


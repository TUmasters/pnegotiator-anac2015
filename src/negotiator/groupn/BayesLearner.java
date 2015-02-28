package negotiator.groupn;

import java.util.*;

import negotiator.DeadlineType;
import negotiator.Domain;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.util.BestBids;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.Bid;
import negotiator.issue.Value;
import java.util.Map.Entry;

/**
 * This is your negotiation party.
 */
public class BayesLearner extends AbstractNegotiationParty {
	
	// State of the agent (am I being a hardliner or am I conceding?)
	public enum AgentState {HARDLINER, CONCEDER};
	public AgentState agentState;
	
	// Parameters of the negotiation
	private UtilitySpace utilitySpace;
	private Timeline timeline;
	
	// Other private fields
	private Domain domain;
	private ArrayList<Objective> objectives;
	private ArrayList<Issue> issues;
	private Bid currentBid = null;
	private Bid lastBid = null;
	private Bid nextBid = null;
	private double concessionFactor = 1;
	private int totalBids = 1;
	
	private ArrayList<TreeSet<ValueFrequency>> valueFrequencies;

    private BestBids bestBids;
	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace Your utility space.
	 * @param deadlines The deadlines set for this negotiation.
	 * @param timeline Value counting from 0 (start) to 1 (end).
	 * @param randomSeed If you use any randomization, use this seed for it.
	 */
	public BayesLearner(UtilitySpace utilitySpace,
                        Map<DeadlineType, Object> deadlines,
                        Timeline timeline,
                        long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);
		
		// Set agent's game parameters
		this.utilitySpace = utilitySpace;
		this.timeline = timeline;
		domain = utilitySpace.getDomain();
		objectives = domain.getObjectives();
		issues = domain.getIssues();
		valueFrequencies = new ArrayList<TreeSet<ValueFrequency>>(issues.size() - 1);
		for(int i = 0; i < issues.size() - 1; ++i) {
			//System.out.println("Adding TreeSet<ValueFrequency> vfs[" + (i) + "]");
			valueFrequencies.add(i, new TreeSet<ValueFrequency>(new VFComp()));
			IssueDiscrete di = (IssueDiscrete)domain.getIssue(i);
			EvaluatorDiscrete ed = (EvaluatorDiscrete)utilitySpace.getEvaluator(i+1);
			for(ValueDiscrete v: di.getValues()) {
				try {
//					System.out.println("\tAdding ValueFrequency element[" + v.value + ", " + (ed.getEvaluation(v) * ed.getWeight()) + "]");
					valueFrequencies.get(i).add(new ValueFrequency(v, ed.getEvaluation(v) * ed.getWeight()));
				} catch (Exception e) {
					System.out.println("Error: " + e);
				}
			}
		}

        try {
            this.bestBids = new BestBids(this.utilitySpace);
            bestBids.genPreferredBids(10);
        } catch(Exception e) {
            e.printStackTrace();
        }
		
		agentState = AgentState.HARDLINER;
		
		// Print stuff for the benefit of our understanding
//		System.out.println("Discount factor: " + utilitySpace.getDiscountFactor());
//		for(int i = 1; i < objectives.size(); ++i)
//		{
//			System.out.println("(Name, Type, Weight): (" + objectives.get(i).getName() + ", " + objectives.get(i).getType() + ", "+ utilitySpace.getWeight(i) + ")");
//		}
//
//		System.out.println("Disagreement point (Discounted):   " + utilitySpace.getReservationValueWithDiscount(timeline));
//		System.out.println("Disagreement point (Undiscounted): " + utilitySpace.getReservationValueUndiscounted());
//		System.out.println("Discount factor: " + utilitySpace.getDiscountFactor());
//		System.out.println("Number of possible bids: " + domain.getNumberOfPossibleBids());
	}
	
	/**
	 * Each round this method gets called and ask you to accept or offer. The first party in
	 * the first round is a bit different, it can only propose an offer.
	 *
	 * @param validActions Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	public Action chooseAction(List<Class> validActions) {
		++totalBids;
		
		// Determine the next bid
		try {
			// If this is the first round, set first bid to max bid
			if(nextBid == null) {
				if(utilitySpace.getMaxUtilityBid() != null) {
					nextBid = utilitySpace.getMaxUtilityBid();
					System.out.println("Next bid set to max:\n" + utilitySpace.getMaxUtilityBid().toString());
					}
					else {
						System.out.println("Max bid was null");
					}
			}
			// Else adjust
			else {
				System.out.println("Time: " + timeline.getTime());
				setState();
				//setConcessionFactor(timeline.getTime());
				decideNextBid();
			}
		} catch (Exception e) { }
		

		// Accepting based on both
		try {
			if(currentBid != null && utilitySpace.getMaxUtilityBid() != null) {
				if((utilitySpace.getUtilityWithDiscount(currentBid, timeline) >= getConcessionUtility())
						&& (utilitySpace.getUtilityWithDiscount(currentBid, timeline) >= (utilitySpace.getUtilityWithDiscount(nextBid, timeline)))) {
					return new Accept();
				}
			}
		} catch (Exception e) { }

		// Print the total utility of the bid we are making and submit it
		lastBid = nextBid;
		System.out.println("Proposing bid with utility: " + utilitySpace.getUtilityWithDiscount(nextBid, timeline));
		updateOurFrequency(nextBid);
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
		if(Action.getBidFromAction(action) != null) {
			currentBid = Action.getBidFromAction(action);
			updateOpponentFrequency(currentBid);
		}
	}
	
	// A dumb function that just sets a scalar concession factor based on the current time
	private double getConcessionUtility() {
		try {
		 double Bj = utilitySpace.getUtility(utilitySpace.getMaxUtilityBid());
    	 double Cj = Bj*timeline.getTime()*timeline.getTime();    
    	 double Uj = Bj-(Cj/objectives.size());
    	//System.out.println("Max  : "+Bj+"Cj   "+Cj+"  time: "+timeline.getTime()+"Bound:"+Uj);
    	return Uj*Uj;
		} catch (Exception e) { return 1.0; }
		/*
		if(time < .3) {
			concessionFactor = .9;
		}
		else if(time >= .3 && time < .8) {
			concessionFactor = .85;
		}
		else if(time >= .8 && time < .93) {
			concessionFactor = .75;
		}
		else if(time >= .93) {
			concessionFactor = .7;
		} */
	}

    public Bid bayesBid() {
        Bid b = null;
        try {
            b = utilitySpace.getMaxUtilityBid();
            for(int i = 0; i < valueFrequencies.size(); ++i) {
                Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
                double highestExpectedUtil = 0.0;
                ValueDiscrete highestExpectedUtilVal = (ValueDiscrete)(lastBid.getValue(i + 1));
                while(itr.hasNext()) {
                    ValueFrequency vfr = itr.next();
                    if(Math.max(highestExpectedUtil, (((double)vfr.opponentFrequency)/(double)totalBids) * vfr.utility) == ((double)vfr.opponentFrequency/(double)totalBids * vfr.utility)) {
                        highestExpectedUtil = ((double)vfr.opponentFrequency/(double)totalBids * vfr.utility);
                        highestExpectedUtilVal = vfr.value;
                    }
                }
                Bid newBid = new Bid(nextBid);
                newBid.setValue(i + 1, highestExpectedUtilVal);
                //double util = utilitySpace.getUtilityWithDiscount(utilitySpace.getMaxUtilityBid(), timeline);
                //util *= concessionFactor;
                //if(utilitySpace.getUtilityWithDiscount(newBid, timeline) >= util) {
                if(utilitySpace.getUtilityWithDiscount(newBid, timeline) >= getConcessionUtility()) {
                    b.setValue(i + 1, highestExpectedUtilVal);
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return b;
    }
	
	// Logic to decide the next bid
	private void decideNextBid() {
		switch(agentState) {
		case HARDLINER:
            System.out.println("MaxRand");
            nextBid = bestBids.getRandomBid(rand);
            break;
		case CONCEDER:
            System.out.println("Bayes");
            nextBid = bayesBid();
		}
	}
	
	// Update the frequency of opponents' proposed values based on a bid an opponent just made
	private void updateOpponentFrequency(Bid bid) {
		for(int i = 0; i < valueFrequencies.size(); ++i) {
		     Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
		     while(itr.hasNext()) {
		    	 ValueFrequency vfr = itr.next();
		    	 try {
		    		 if(vfr.value.toString().compareTo(((ValueDiscrete)bid.getValue(i+1)).toString()) == 0) {
			    		 vfr.opponentFrequency++;
			    		 break;
			    	 }
		    	 } catch(Exception e) {
		    		 System.out.println("Error: " + e);
		    	 }
		     }
		}
	}
	
	// Update the frequency of our proposed values based on a bid we're about to make
	private void updateOurFrequency(Bid bid) {
		for(int i = 0; i < valueFrequencies.size(); ++i) {
		     Iterator<ValueFrequency> itr = valueFrequencies.get(i).descendingIterator();
		     while(itr.hasNext()) {
		    	 ValueFrequency vfr = itr.next();
		    	 try {
		    		 if(vfr.value.toString().compareTo(((ValueDiscrete)bid.getValue(i+1)).toString()) == 0) {
			    		 vfr.ourFrequency++;
			    		 break;
			    	 }
		    	 } catch(Exception e) {
		    		 System.out.println("Error: " + e);
		    	 }
		     }
		}
	}
	
	private void setState() {
		if(timeline.getTime() >= .5) {
			agentState = AgentState.CONCEDER;
		}
	}
}

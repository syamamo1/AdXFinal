package agent;

import java.util.Collections;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintStream;
import java.time.LocalTime;


import com.google.common.collect.ImmutableMap;

import adx.agent.AgentLogic;
import adx.exceptions.AdXException;
import adx.server.OfflineGameServer;
import adx.structures.SimpleBidEntry;
import adx.util.AgentStartupUtil;
import adx.structures.Campaign;
import adx.structures.MarketSegment;
import adx.variants.ndaysgame.NDaysAdBidBundle;
import adx.variants.ndaysgame.NDaysNCampaignsAgent;
import adx.variants.ndaysgame.NDaysNCampaignsGameServerOffline;
import adx.variants.ndaysgame.Tier1NDaysNCampaignsAgent;


public class MyNDaysNCampaignsAgent extends NDaysNCampaignsAgent {
	
	class SegmentTuple implements Comparable<SegmentTuple> {
		private MarketSegment segment;
		private Double expectedBid;
		
		public MarketSegment getMarketSegment() {
			return segment;
		}
		
		public Double getExpectedBid() {
			return expectedBid;
		}
		
		public SegmentTuple(MarketSegment segment, double expectedBid) {
			this.segment = segment;
			this.expectedBid = expectedBid;
		}

		@Override
		public int compareTo(SegmentTuple arg0) {
			return expectedBid.compareTo(arg0.getExpectedBid());
		}
		
	}
	
	
	private static final String NAME = "AlexSean";
	private final int NUMBER_OF_DAYS = 10;
	private final int NUMBER_OF_PLAYERS = 10;
	private Map<MarketSegment, Map<Integer, List<Double>>> map;

	public MyNDaysNCampaignsAgent() {
		redirectPrints();
		// Initialize
		map = new HashMap<>();
	}
	
	@Override
	protected void onNewGame() {
		try {
			map.clear();
			
			int numberOfCampaignSegments = MarketSegment.values().length - 6;
			double probOfCampaignSegment = 1.0 / ((double)numberOfCampaignSegments);
			
			for (MarketSegment m : MarketSegment.values()) {
				if (this.determineClass(m) == 3) {
					Map<Integer, List<Double>> days = new HashMap<>();
					for(int day = 1; day <= NUMBER_OF_DAYS; day++) {
						List<Double> probabilities = new ArrayList<>();
						days.put(day, probabilities);
					}
					map.put(m, days);
				}
			}
			
			// For each day in the auction, there is p = min(1, Q) chance that each player is 
			// assigned a 1 day random auction. Q is approximated by an exponential decay 
			// function.
			for (MarketSegment m : MarketSegment.values()) {
				if (this.determineClass(m) >= 2) {
					Set<MarketSegment> subsets = this.getMarketSubsets(m);
					for(int day = 1; day <= NUMBER_OF_DAYS; day++) {
						double expectedNumberOfFreeAuctions = NUMBER_OF_PLAYERS * this.getApproximateQ(day);
						for(MarketSegment s : subsets) {
							if (this.determineClass(s) == 3) {
								this.map.get(s).get(day).addAll(Collections.nCopies((int)expectedNumberOfFreeAuctions, probOfCampaignSegment));
							}
						}
					}
				}
			}
			
//			for(MarketSegment m: MarketSegment.values()) {
//				if (this.determineClass(m) == 3) {
//					for(int day = 0; day < numberOfDays; day++) { 
//						System.out.print("Market Segment " + m + " day " + day + ": ");
//						for(Double prob: this.map.get(m).get(day)) {
//							System.out.print(prob + " ");
//						}
//						System.out.println("");
//					}
//				}
//			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private double getApproximateQ(int day) {
		return Math.exp(-0.1 * ((double)(day-1)));
	}
	
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		Set<NDaysAdBidBundle> bundles = new HashSet<>();
		
		int currentDay = this.getCurrentDay();
		
		
		Set<Campaign> activeCampaigns = this.getActiveCampaigns();
		
		for (Campaign c : activeCampaigns) {
			Set<SimpleBidEntry> bids = new HashSet<>();
			double budget = c.getBudget();
			int reach = c.getReach();
			double moneySpent = getCumulativeCost(c);
			int impressions = getCumulativeReach(c);
			
			double budgetLeft = Math.max((budget-moneySpent), 1);
			double budgetPerImpression = Math.max((budget-moneySpent)/(reach-impressions), 0.01);
			double effectiveReachLeft = Math.sqrt(Math.pow(1.38442, 2) - Math.pow(effectiveReach(impressions, reach), 2));	

			MarketSegment m = c.getMarketSegment();
			
			ArrayList<SegmentTuple> segments = new ArrayList<>();
			Set<MarketSegment> subsets = this.getMarketSubsets(m);
			for (MarketSegment s: subsets) {
				if (this.determineClass(s) == 3) {
					List<Double> probabilities = this.map.get(s).get(currentDay);
					
					double sum = probabilities.stream()
						    .mapToDouble(a -> a)
						    .sum();
					
					double playersExceptSelf = Math.max(0.01,  sum-1);
					double expectedBid = playersExceptSelf / (playersExceptSelf+1);
					
					SegmentTuple t = new SegmentTuple(s, expectedBid);
					segments.add(t);
				}
			}
			
			for (SegmentTuple t : segments) {
				double expectedBid = t.getExpectedBid();
				double inverse = 1/expectedBid;
				double bid = inverse * effectiveReachLeft;
				bid = Math.max(bid, 0.5);
				bid = Math.min(bid, budgetPerImpression);
				
				SimpleBidEntry bidEntry = new SimpleBidEntry(
					t.getMarketSegment(), 
					bid, 
					budgetLeft*bid // segment limit 
				);
				
				bids.add(bidEntry);
			}
			
			NDaysAdBidBundle bundle = new NDaysAdBidBundle( 
				c.getId(), 
				budgetLeft / c.getEndDay() - currentDay + 1, // campaign limit
				bids
			);
		
			bundles.add(bundle);
		}
		
		// If we are competing with ourself reduce the bid associated with the less urgent campaign to zero


		return bundles;
	}

	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {

		Map<Campaign, Double> bids = new HashMap<>();
		// double qualityScore = getQualityScore();
		
		// If we win, smallest our budget can be is what we bid
		double discount1 = 0.70;
		double discount2 = 0.85;
		double discount3 = 0.90;
		for (Campaign c : campaignsForAuction) {

			int reach = c.getReach();
			double ratio = 1;
			MarketSegment marketSegment = c.getMarketSegment();
			
			// If campaigns up for auction overlap with each other
			for (Campaign c0 : campaignsForAuction) {
				if (c0.getId() != c.getId()) {
					// Bidding on is more specific -> bid more aggressively
					if (MarketSegment.marketSegmentSubset(marketSegment, c0.getMarketSegment())) {
						ratio = ratio * discount1;
					}			
					// Bidding on is less specific -> bid less aggressively
					else if (MarketSegment.marketSegmentSubset(c0.getMarketSegment(), marketSegment)) {
						ratio = ratio * discount2;
					}			
					// If we have a campaign that partially overlaps with the current campaign
					else if (hasPartialOverlap(marketSegment, c0.getMarketSegment())) {
						ratio = ratio * discount3;
					}
				}
			}

			// If campaigns up for auction overlap with our active campaigns
			for (Campaign myC : this.getActiveCampaigns()) {
				// Bidding on is more specific -> bid more aggressively
				if (MarketSegment.marketSegmentSubset(marketSegment, myC.getMarketSegment())) {
					ratio = ratio * discount1;
				}			
				// Bidding on is less specific -> bid less aggressively
				else if (MarketSegment.marketSegmentSubset(myC.getMarketSegment(), marketSegment)) {
					ratio = ratio * discount2;
				}			
				// If we have a campaign that partially overlaps with the current campaign
				else if (hasPartialOverlap(marketSegment, myC.getMarketSegment())) {
					ratio = ratio * discount3;
				}
			
			bids.put(c, reach*ratio);
			}
		}
		
		// Based on the campaigns that are being auctioned off, update our map
		for (Campaign c : campaignsForAuction) {
			MarketSegment m = c.getMarketSegment();
			int endDay = c.getEndDay();
			int currentDay = this.getCurrentDay();
			
			Set<MarketSegment> subsets = this.getMarketSubsets(m);
			for(int day = currentDay; day <= endDay; day++) {
				for(MarketSegment s : subsets) {
					if (this.determineClass(s) == 3) {
						// For each day that the auction is live, we know that there will be demand on this
						// market segment
						this.map.get(s).get(day).add(1.0);
					}
				}
			}
		}
		
		return bids;
	}
	
	private Set<MarketSegment> getMarketSubsets(MarketSegment segment) throws AdXException {
		Set<MarketSegment> segments = new HashSet<>();
		
		for(MarketSegment m: MarketSegment.values()) {
			if (MarketSegment.marketSegmentSubset(segment, m)) {
				segments.add(m);
			}
		}
		
		return segments;
	}


	// If at least one attribute is equal
	// for example, both have "OLD" or "RICH"
	public boolean hasPartialOverlap(MarketSegment m1, MarketSegment m2) throws AdXException {
		
		MarketSegment[] maleSegments = { MarketSegment.MALE_LOW_INCOME, MarketSegment.MALE_HIGH_INCOME, MarketSegment.MALE_OLD, MarketSegment.MALE_YOUNG };
		MarketSegment[] femaleSegments = { MarketSegment.FEMALE_YOUNG, MarketSegment.FEMALE_OLD, MarketSegment.FEMALE_LOW_INCOME, MarketSegment.FEMALE_HIGH_INCOME };
		MarketSegment[] youngSegments = { MarketSegment.FEMALE_YOUNG, MarketSegment.MALE_YOUNG, MarketSegment.YOUNG_LOW_INCOME, MarketSegment.YOUNG_HIGH_INCOME };
		MarketSegment[] oldSegments = { MarketSegment.FEMALE_OLD, MarketSegment.MALE_OLD, MarketSegment.OLD_LOW_INCOME, MarketSegment.OLD_HIGH_INCOME };
		MarketSegment[] lowIncomeSegments = { MarketSegment.MALE_LOW_INCOME, MarketSegment.FEMALE_LOW_INCOME, MarketSegment.YOUNG_LOW_INCOME, MarketSegment.OLD_LOW_INCOME };
		MarketSegment[] highIncomeSegments = { MarketSegment.MALE_HIGH_INCOME, MarketSegment.FEMALE_HIGH_INCOME, MarketSegment.YOUNG_HIGH_INCOME, MarketSegment.OLD_HIGH_INCOME };
		
		MarketSegment[] sub1;
		MarketSegment[] sub2;
		if (m1.equals(MarketSegment.MALE_LOW_INCOME)) {
			sub1 = maleSegments; sub2 = lowIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.MALE_HIGH_INCOME)) {
			sub1 = maleSegments; sub2 = highIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.FEMALE_YOUNG)) {
			sub1 = femaleSegments; sub2 = youngSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.FEMALE_OLD)) {
			sub1 = femaleSegments; sub2 = oldSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.FEMALE_LOW_INCOME)) {
			sub1 = femaleSegments; sub2 = lowIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.FEMALE_HIGH_INCOME)) {
			sub1 = femaleSegments; sub2 = highIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.YOUNG_LOW_INCOME)) {
			sub1 = youngSegments; sub2 = lowIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.YOUNG_HIGH_INCOME)) {
			sub1 = youngSegments; sub2 = highIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.OLD_LOW_INCOME)) {
			sub1 = oldSegments; sub2 = lowIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.OLD_HIGH_INCOME)) {
			sub1 = oldSegments; sub2 = highIncomeSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.MALE_OLD)) {
			sub1 = maleSegments; sub2 = oldSegments;
			if (inArrays(m2, sub1, sub2)) { return true; }

		} else if (m1.equals(MarketSegment.MALE_YOUNG)) {
			sub1 = maleSegments; sub2 = youngSegments; 
			if (inArrays(m2, sub1, sub2)) { return true; }
		}
		return false;
	}
		
	// If (m1 subset m2) or (m2 subset m1)
	public boolean inArrays(MarketSegment m, MarketSegment[] sub1, MarketSegment[] sub2) {
		for (MarketSegment m2: sub1) {
			if (m.equals(m2)) {
				return true;
			}
		}
		for (MarketSegment m2: sub2) {
			if (m.equals(m2)) {
				return true;
			}
		}
		return false;
	}

	// Sigmoidal that has (0, 0.18), (1, 1) 
	public double mapBid(double x) {
		double exp = -Math.exp(2) * (x - 0.2);
		return 1/(1 + Math.exp(exp));
	}

	// Takes in current impressions x, reach R
	public double getDerivative(int x, int R) {
		double a = 4.08577;
		double b = 3.08577;
		double bot = Math.pow((a*(x/R) - b), 2);
		double dER_dx = (2/R) * (1/(1 + bot));

		return dER_dx;
	}


	// Redirect console prints to file so we can actually read it
	// Thanks: https://www.tutorialspoint.com/redirecting-system-out-println-output-to-a-file-in-java
	public void redirectPrints() {
	  try {
		File file = new File("console");
		PrintStream stream = new PrintStream(file);
		System.out.println("\nSaving console to "+file.getAbsolutePath());
		System.setOut(stream);
        LocalTime currentTime = LocalTime.now();
        System.out.println("Current time: " + currentTime);
	  }
	  catch (IOException e) {
		System.out.println("An error occurred: " + e.getMessage());
	  }
   }

	// Returns number of users a Market Segment has
	public int numberUsers(MarketSegment m) {
		if (m.equals(MarketSegment.MALE)) {
			return 4956;
		}
		if (m.equals(MarketSegment.FEMALE)) {
			return 5044;
		}
		if (m.equals(MarketSegment.YOUNG)) {
			return 4589;
		}
		if (m.equals(MarketSegment.OLD)) {
			return 5411;
		}
		if (m.equals(MarketSegment.LOW_INCOME)) {
			return 8012;
		}
		if (m.equals(MarketSegment.HIGH_INCOME)) {
			return 1988;
		}
		if (m.equals(MarketSegment.MALE_YOUNG)) {
			return 2353;
		}
		if (m.equals(MarketSegment.MALE_OLD)) {
			return 2603;
		}
		if (m.equals(MarketSegment.MALE_LOW_INCOME)) {
			return 3631;
		}
		if (m.equals(MarketSegment.MALE_HIGH_INCOME)) {
			return 1325;
		}
		if (m.equals(MarketSegment.FEMALE_YOUNG)) {
			return 2236;
		}
		if (m.equals(MarketSegment.FEMALE_OLD)) {
			return 2808;
		}
		if (m.equals(MarketSegment.FEMALE_LOW_INCOME)) {
			return 4381;
		}
		if (m.equals(MarketSegment.FEMALE_HIGH_INCOME)) {
			return 663;
		}
		if (m.equals(MarketSegment.YOUNG_LOW_INCOME)) {
			return 3816;
		}
		if (m.equals(MarketSegment.YOUNG_HIGH_INCOME)) {
			return 773;
		}
		if (m.equals(MarketSegment.OLD_LOW_INCOME)) {
			return 4196;
		}
		if (m.equals(MarketSegment.OLD_HIGH_INCOME)) {
			return 1215;
		}
		if (m.equals(MarketSegment.MALE_YOUNG_LOW_INCOME)) {
			return 1836;
		}
		if (m.equals(MarketSegment.MALE_YOUNG_HIGH_INCOME)) {
			return 517;
		}
		if (m.equals(MarketSegment.MALE_OLD_LOW_INCOME)) {
			return 1795;
		}
		if (m.equals(MarketSegment.MALE_OLD_HIGH_INCOME)) {
			return 808;
		}
		if (m.equals(MarketSegment.FEMALE_YOUNG_LOW_INCOME)) {
			return 1980;
		}
		if (m.equals(MarketSegment.FEMALE_YOUNG_HIGH_INCOME)) {
			return 256;
		}
		if (m.equals(MarketSegment.FEMALE_OLD_LOW_INCOME)) {
			return 2401;
		}
		if (m.equals(MarketSegment.FEMALE_OLD_HIGH_INCOME)) {
			return 407;
		}
		return 0; // Should never happen
	}

	// Returns 1 if 1/3 fields (least specific)
	// Returns 2 if 2/3 fields (mid specific)
	// Returns 3 if 3/3 fields (more specific)
	public int determineClass(MarketSegment m) {
		MarketSegment[] singles = {
			MarketSegment.MALE, MarketSegment.FEMALE, MarketSegment.YOUNG, 
			MarketSegment.OLD, MarketSegment.LOW_INCOME, MarketSegment.HIGH_INCOME};

		MarketSegment[] doubles = {
			MarketSegment.MALE_LOW_INCOME, MarketSegment.MALE_HIGH_INCOME, MarketSegment.FEMALE_YOUNG, 
			MarketSegment.FEMALE_OLD, MarketSegment.FEMALE_LOW_INCOME, MarketSegment.FEMALE_HIGH_INCOME,
			MarketSegment.YOUNG_LOW_INCOME, MarketSegment.YOUNG_HIGH_INCOME, MarketSegment.OLD_LOW_INCOME,
			MarketSegment.OLD_HIGH_INCOME, MarketSegment.MALE_OLD, MarketSegment.MALE_YOUNG};

		MarketSegment[] triples = {
			MarketSegment.MALE_YOUNG_LOW_INCOME, MarketSegment.MALE_YOUNG_HIGH_INCOME, MarketSegment.MALE_OLD_LOW_INCOME, 
			MarketSegment.MALE_OLD_HIGH_INCOME, MarketSegment.FEMALE_YOUNG_LOW_INCOME, MarketSegment.FEMALE_YOUNG_HIGH_INCOME,
			MarketSegment.FEMALE_OLD_LOW_INCOME, MarketSegment.FEMALE_OLD_HIGH_INCOME};
		
		// Iterate and find...
		for (int i=0; i < singles.length; i ++) {
			if (m.equals((singles[i]))) {
				return 1;
			}
		}
		for (int i=0; i < doubles.length; i ++) {
			if (m.equals((doubles[i]))) {
				return 2;
			}
		}
		for (int i=0; i < triples.length; i ++) {
			if (m.equals((triples[i]))) {
				return 3;
			}
		}
		return 0; // Should never happen
	}

	public static void main(String[] args) throws IOException, AdXException {
		// Here's an opportunity to test offline against some TA agents. Just run
		// this file in Eclipse to do so.
		// Feel free to change the type of agents.
		// Note: this runs offline, so:
		// a) It's much faster than the online test; don't worry if there's no delays.
		// b) You should still run the test script mentioned in the handout to make sure
		// your agent works online.
		
		if (args.length == 0) {
			Map<String, AgentLogic> test_agents = new ImmutableMap.Builder<String, AgentLogic>()
					.put(NAME, new MyNDaysNCampaignsAgent())
//					.put("TwoKBot1", new TwoKBot())
//					.put("TwoKBot2", new TwoKBot())
					.put("bot_1", new Tier1NDaysNCampaignsAgent())
					.put("bot_2", new Tier1NDaysNCampaignsAgent())
					.put("bot_3", new Tier1NDaysNCampaignsAgent())
					.put("bot_4", new Tier1NDaysNCampaignsAgent())
					.put("bot_5", new Tier1NDaysNCampaignsAgent())
					.put("bot_6", new Tier1NDaysNCampaignsAgent())
					.put("bot_7", new Tier1NDaysNCampaignsAgent())
					.put("bot_8", new Tier1NDaysNCampaignsAgent())
					.put("bot_9", new Tier1NDaysNCampaignsAgent())
					.build();

			// Don't change this.
			OfflineGameServer.initParams(new String[] { "offline_config.ini", "CS1951K-FINAL" });
			
			AgentStartupUtil.testOffline(test_agents, new OfflineGameServer());
		} else {
			// Don't change this.
			AgentStartupUtil.startOnline(new MyNDaysNCampaignsAgent(), args, NAME);
		}
	}

}

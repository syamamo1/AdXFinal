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
	private static final String NAME = "AlexSean"; // TODO: enter a name. please remember to submit the Google form.
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
		
		for (Campaign c : this.getActiveCampaigns()) {
			MarketSegment m = c.getMarketSegment();
			
			Map<MarketSegment, Double> expectedMaxBids = new HashMap<>();
			
			Set<MarketSegment> subsets = this.getMarketSubsets(m);
			for (MarketSegment s: subsets) {
				if (this.determineClass(s) == 3) {
					List<Double> probabilities = this.map.get(s).get(currentDay);
					
					double sum = probabilities.stream()
						    .mapToDouble(a -> a)
						    .sum();
					// Assumes other bidders use a uniform distribution between 0 and 1. Can 
					// probably switch to something more sophisticated.
					double excludingSelf = Math.max(0, sum - 1);
					double expectedMaxBid = excludingSelf / (excludingSelf + 1);
					expectedMaxBids.put(s, expectedMaxBid);
				}
			}
		}
		
		for (Campaign c : this.getActiveCampaigns()) {

			Set<SimpleBidEntry> bids = new HashSet<>();
			MarketSegment marketSegment = c.getMarketSegment();
			double budget = c.getBudget();
			int reach = c.getReach();
			double moneySpent = getCumulativeCost(c);
			int impressions = getCumulativeReach(c);
			double budgetLeft = Math.max((budget-moneySpent), 1);
			int startDay = c.getStartDay();
			int endDay = c.getEndDay();
			int day = getCurrentDay();

			double bid = 1;
			// do something with the derivative here...
			if (impressions > reach) {
				bid = 0.001;
			}
			if (startDay == endDay) {
				bid = 0.9;
			}
			else {
				double num = (budget-moneySpent) * (day - startDay + 0.5);
				double den = (reach-impressions) * (endDay - startDay + 0.5);
				bid = Math.sin(num/den);
				bid = Math.max(bid, 0.5);
			}


			for(MarketSegment m : MarketSegment.values()) {
				// Our segment
				if (marketSegment.equals(m)) {
					SimpleBidEntry bidEntry = new SimpleBidEntry(
						m, 
						bid, 
						budgetLeft // segment limit 
					);
					bids.add(bidEntry);
				}

				// Segments we are subsegments of (Tier 3 segments)
				// else if (MarketSegment.marketSegmentSubset(marketSegment, m)) {
				// 	double bid = 0.1;
				// 	SimpleBidEntry bidEntry = new SimpleBidEntry(
				// 		m, 
				// 		bid*ratio, 
				// 		budgetLeft*bid // segment limit 
				// 	);
				// 	bids.add(bidEntry);
				// }

				// if (m.equals(marketSegment)) {
				// 	SimpleBidEntry bidEntry = new SimpleBidEntry(m,bid,bid*thisDayBudget);
				// 	bids.add(bidEntry);			
				// }

				else { // Don't bid on segments that don't add to our reach
					SimpleBidEntry bidEntry = new SimpleBidEntry(m,0.0,1.0);
					bids.add(bidEntry);
				}
			}
		
			NDaysAdBidBundle bundle = new NDaysAdBidBundle( 
				c.getId(), 
				budgetLeft*bid, // campaign limit
				bids
			);
			
			bundles.add(bundle);
		}

		return bundles;
	}

	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {

		Map<Campaign, Double> bids = new HashMap<>();
		
		for (Campaign c : campaignsForAuction) {
			bids.put(c, c.getReach()*0.7);
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
					.put("bot_1", new Tier1NDaysNCampaignsAgent())
					.put("bot_2", new Tier1NDaysNCampaignsAgent())
					.put("bot_3", new Tier1NDaysNCampaignsAgent())
					.put("bot_4", new Tier1NDaysNCampaignsAgent())
					.put("bot_5", new Tier1NDaysNCampaignsAgent())
					.put("bot_6", new Tier1NDaysNCampaignsAgent())
					.put("bot_7", new Tier1NDaysNCampaignsAgent())
					.put("bot_8", new Tier1NDaysNCampaignsAgent())
					.put("bot_9", new Tier1NDaysNCampaignsAgent()).build();

			// Don't change this.
			OfflineGameServer.initParams(new String[] { "offline_config.ini", "CS1951K-FINAL" });
			
			AgentStartupUtil.testOffline(test_agents, new OfflineGameServer());
		} else {
			// Don't change this.
			AgentStartupUtil.startOnline(new MyNDaysNCampaignsAgent(), args, NAME);
		}
	}

}

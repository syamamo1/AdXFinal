package agent;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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

	public MyNDaysNCampaignsAgent() {
		// TODO: fill this in (if necessary)
		redirectPrints();
	}
	
	@Override
	protected void onNewGame() {
		// TODO: fill this in (if necessary)
		
	}
	
	@Override
	protected Set<NDaysAdBidBundle> getAdBids() throws AdXException {
		// TODO: fill this in
		
		Set<NDaysAdBidBundle> bundles = new HashSet<>();
		
		for (Campaign c : this.getActiveCampaigns()) {

			Set<SimpleBidEntry> bids = new HashSet<>();
			MarketSegment marketSegment = c.getMarketSegment();

			for(MarketSegment m : MarketSegment.values()) {

				// if (MarketSegment.marketSegmentSubset(m, marketSegment)) {
				// 	double bid = 1;
				// 	SimpleBidEntry bidEntry = new SimpleBidEntry(
				// 		m, 
				// 		bid, 
				// 		c.getBudget() // segment limit 
				// 	);
				// 	bids.add(bidEntry);
				// }
				if (m.equals(marketSegment)) {
					SimpleBidEntry bidEntry = new SimpleBidEntry(m,0.9,c.getBudget());
					bids.add(bidEntry);			
				}

				else { // Don't bid on segments that don't add to our reach
					SimpleBidEntry bidEntry = new SimpleBidEntry(m,0.0,1.0);
					bids.add(bidEntry);
				}
			}
		
			NDaysAdBidBundle bundle = new NDaysAdBidBundle( 
				c.getId(), 
				c.getBudget(), // campaign limit
				bids
			);
			
			bundles.add(bundle);
		}

		return bundles;
	}

	@Override
	protected Map<Campaign, Double> getCampaignBids(Set<Campaign> campaignsForAuction) throws AdXException {
		// TODO: fill this in
		
		Map<Campaign, Double> bids = new HashMap<>();
		
		for (Campaign c : campaignsForAuction) {
			bids.put(c, Math.random());
		}
		
		return bids;
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
			MarketSegment.OLD_HIGH_INCOME};

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

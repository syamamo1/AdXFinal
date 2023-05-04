package lab08;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
// import java.util.HashSet;

import org.apache.commons.lang3.ObjectUtils.Null;

import com.google.common.collect.ImmutableMap;

import adx.agent.AgentLogic;
import adx.exceptions.AdXException;
import adx.server.OfflineGameServer;
import adx.structures.Campaign;
import adx.structures.MarketSegment;
import adx.structures.SimpleBidEntry;
import adx.util.AgentStartupUtil;
import adx.variants.onedaygame.OneDayAgent;
import adx.variants.onedaygame.OneDayBidBundle;
import adx.variants.onedaygame.SimpleOneDayAgent;

public class MyOneDayAgent extends OneDayAgent {
	private static final String NAME = "B01326006"; // TODO: enter a name. please user either your CS login or Banner ID.
	
	public MyOneDayAgent() {
		// TODO: fill this in (if necessary)
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

	// Put in try catch...
	@Override
	protected OneDayBidBundle getBidBundle() {
		// TODO: fill this in
		
		// Game parameters, unused right now
		int nCompetition = 9; // number of competitors
		int nSegments = 20; // number of segments (at least Class 2)

		// Campaign variables/parameters
		Campaign campaign = this.getCampaign();
		MarketSegment marketSegment = campaign.getMarketSegment();
		Set<SimpleBidEntry> bidEntries = new HashSet<>();
		int segmentClass = determineClass(marketSegment);
		int reach = campaign.getReach();

		// How many users are in subsets of our segment? Used in Class 3 
		// More = less competition
		int numSubset = 0;
		for (MarketSegment ms : MarketSegment.values()) {
			if (MarketSegment.marketSegmentSubset(marketSegment, ms)) {
				// Only count the 20 possible classes, not single ones
				if (determineClass(ms) >= 2) {
					numSubset += numberUsers(ms);
				}
			}
		}

		// How many segments can we bid on?
		// How many users are in those segments? Used in Class 2
		// More = less competition
		int numPossible = 0;
		int numPossibleUsers = 0;
		for (MarketSegment ms : MarketSegment.values()) {
			if (MarketSegment.marketSegmentSubset(ms, marketSegment)) {
				numPossible += 1;
				numPossibleUsers += numberUsers(ms);
			}
		}

		// TODO: this surely needs tuning (add random coefficients and test performance)
		double ratio = (nCompetition/nSegments)*(numSubset/numPossibleUsers);
		double bid = 1.0*ratio; 
		// Set floor/ceiling on ratio?
		// if (ratio > 0.9) {
		// 	ratio = 0.9;
		// }

		// Iterate different segments
		for (MarketSegment m : MarketSegment.values()) {

			// ------------------------------------------------------------
			// Case 1: Class = 3
			// Our only option is to win this one type of client
			if ((segmentClass == 3) && (m.equals(marketSegment))) {
				
				// Shade spending limit so we don't spend money for 0 reward
				// This assumes that we are getting each user for 90% of our bid
				double spendingLimit = 0.9*campaign.getBudget()*ratio; 
				SimpleBidEntry bidEntry = new SimpleBidEntry(m, bid, spendingLimit);
				bidEntries.add(bidEntry);

			} // Bid 0 for stuff we don't value
			else if (segmentClass == 3) {
				SimpleBidEntry bidEntry = new SimpleBidEntry(m, 0.0, 0.0);
				bidEntries.add(bidEntry);
			}

			// ------------------------------------------------------------
			// Case 2: Class = 2
			// If our MS is a subset of this MS
			if (MarketSegment.marketSegmentSubset(m, marketSegment)) {
				// Multiply by proportion of possible users are in this segment
				// This assumes that we are getting each user for 90% of our bid
				int nUsersHere = numberUsers(m);
				double spendingLimit = 0.9*(campaign.getBudget()*ratio)*(nUsersHere/numPossible);
				SimpleBidEntry bidEntry = new SimpleBidEntry(m, bid, spendingLimit);
				bidEntries.add(bidEntry);

			} // Bid 0 for stuff we don't value
			else { 
				SimpleBidEntry bidEntry = new SimpleBidEntry(m, 0.0, 0.0);
				bidEntries.add(bidEntry);
			}
		}

		// Make bid bundle to return
		OneDayBidBundle bidBundle = new OneDayBidBundle(
			campaign.getId(),
			campaign.getBudget(),
			bidEntries
		);

		return bidBundle;
	}
	
	public static void main(String[] args) throws IOException, AdXException {
		// Here's an opportunity to test offline against some mystery agents. Just run this file in Eclipse to do so.
		// Feel free to change the type of agents. SimpleOneDayAgent is a simple pre-defined bot.
		// Note: this runs offline, so:
		//			a) It's much faster than the online test; don't worry if there's no delays.
		//			b) You should still run the test script mentioned in the handout to make sure your agent works online.
		if (args.length == 0) {
			Map<String, AgentLogic> test_agents = new ImmutableMap.Builder<String, AgentLogic>()
					.put("me", new MyOneDayAgent())
					.put("opponent_1", new SimpleOneDayAgent())
					.put("opponent_2", new SimpleOneDayAgent())
					.put("opponent_3", new SimpleOneDayAgent())
					.put("opponent_4", new SimpleOneDayAgent())
					.put("opponent_5", new SimpleOneDayAgent())
					.put("opponent_6", new SimpleOneDayAgent())
					.put("opponent_7", new SimpleOneDayAgent())
					.put("opponent_8", new SimpleOneDayAgent())
					.put("opponent_9", new SimpleOneDayAgent())
					.build();
			
			// Don't change this.
			OfflineGameServer.initParams(new String[] {"offline_config.ini", "ONE-DAY-ONE-CAMPAIGN"});
			AgentStartupUtil.testOffline(test_agents, new OfflineGameServer());
		} else {
			// Don't change this.
			AgentStartupUtil.startOnline(new MyOneDayAgent(), args, NAME);
		}
	}
	
}

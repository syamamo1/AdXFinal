from parse_output import get_data
from graphers import graph_performance, make_histograms
from utils import effective_reach, extract_array

import numpy as np
import pandas as pd

# Either 2 players or all players
def get_players():
    simple = 1
    all = 0
    
    if simple: 
        players = ['AlexSean', 'bot_1']

    elif all:
        players = []
        for i in range(0, 10):
            if i == 0:
                player = 'AlexSean'
            else:
                player = f'bot_{i}'
            players.append(player)

    return players


# Might be interesting 
def compute_stats(campaigns, players, filename):
    df = pd.DataFrame(columns=players)
    for i, player in enumerate(players):
        player_campaigns = campaigns[player]
        impressions = extract_array(player_campaigns, 'impressions')
        reach = extract_array(player_campaigns, 'reach')
        cost = extract_array(player_campaigns, 'cost')
        budget = extract_array(player_campaigns, 'budget')

        profits = np.vectorize(effective_reach)(impressions, reach) * budget - cost
        pos_profits = profits > 0
        num_profitable = profits[pos_profits].shape[0]

        num_zero = profits[profits == 0].shape[0]

        neg_profits = profits < 0
        num_negative = profits[neg_profits].shape[0]

        # TRACK EFFECTIVE REACH FOR NEGATIVES!

        sum_imp = np.sum(impressions)
        sum_reach = np.sum(reach)
        sum_cost = np.sum(cost)

        num_campaigns = int(extract_array(player_campaigns, 'impressions').shape[0])
        er = effective_reach(sum_imp, sum_reach)
        cpi = round(sum_cost/sum_imp, 3)
        df.loc[:, player] = [num_campaigns, num_profitable, num_zero,
                             num_negative, er, sum_imp, cpi]

    df.index = ['Num Campaigns', 'Num Profitable',  'Num Zero', 
                'Num Negative', 'Eff. R.', 'Total Imp.', '$/Imp.']
    print('\n', df, '\n')
    df.to_excel(filename)
    print(f'Saved to {filename}')


# Run this file to analyze model performance
def main():
    players = get_players()
    log_filename = 'console'
    stats_filename = 'stats.xlsx'

    data, campaigns, final_results = get_data(log_filename)
    compute_stats(campaigns, players, stats_filename)
    make_histograms(campaigns, players)
    graph_performance(data, players)




if __name__ == '__main__':
    main()
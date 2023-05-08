from parse_output import get_data
from graphers import graph_performance, make_general_histograms, make_detailed_histograms
from utils import effective_reach, extract_array, get_histogram_data

import numpy as np
import pandas as pd


# Either just us, us and bot_1, or all players
def get_players(solo, two, all):
    
    if solo:
        players = ['AlexSean']

    elif two: 
        players = ['AlexSean', 'bot_1']

    elif all:
        players = [
            'AlexSean',
            'TwoKBot1',
            'TwoKBot2',
            'bot_1',
            'bot_2',
            'bot_3',
            'bot_4',
            'bot_5',
            'bot_6',
            'bot_7',
        ]

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
        total_flow = np.linalg.norm(profits, ord=1)

        pos_profits = profits > 0
        num_profitable = profits[pos_profits].shape[0]
        pc_profitable = round(num_profitable / profits.shape[0], 3) * 100
        weight_positive = round(np.linalg.norm(profits[pos_profits], ord=1)*100/total_flow)

        num_zero = profits[profits == 0].shape[0]
        pc_zero = round(num_zero / profits.shape[0], 3) * 100

        neg_profits = profits < 0
        num_negative = profits[neg_profits].shape[0]
        pc_negative = round(num_negative / profits.shape[0], 3) * 100
        weight_negative = round(np.linalg.norm(profits[neg_profits], ord=1)*100/total_flow)

        sum_imp = np.sum(impressions)
        sum_reach = np.sum(reach)
        sum_cost = np.sum(cost)

        num_campaigns = int(extract_array(player_campaigns, 'impressions').shape[0])
        er = effective_reach(sum_imp, sum_reach)
        cpi = round(sum_cost/sum_imp, 3)
        df.loc[:, player] = [num_campaigns, weight_positive, weight_negative,
                             pc_profitable, pc_zero, pc_negative,
                             num_profitable, num_zero, num_negative, er, sum_imp, cpi]

    df.index = ['Num Campaigns', 'Weight Positive', 'Weight Negative',
                '% Profitable', '% Zero', '% Negative',
                'Num Profitable',  'Num Zero', 'Num Negative', 
                'Eff. R.', 'Total Imp.', '$/Imp.']
    
    print('\n', df, '\n')
    df.to_excel(filename)
    print(f'Saved to {filename}')


# Run this file to analyze model performance
def main():
    # Define players
    just_me = get_players(1, 0, 0)
    me_and_bot1 = get_players(0, 1, 0)
    all_players = get_players(0, 0, 1)

    # Define filenames
    log_filename = 'console'
    stats_filename = 'stats.xlsx'

    # Get data
    data, campaigns, final_results = get_data(log_filename, all_players)
    histogram_data = get_histogram_data(campaigns, just_me)
    
    # Print results
    print(final_results)
    compute_stats(campaigns, all_players, stats_filename)
    make_general_histograms(histogram_data, just_me)
    make_detailed_histograms(histogram_data, just_me)
    graph_performance(data, all_players)




if __name__ == '__main__':
    main()
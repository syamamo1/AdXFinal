import numpy as np
from math import atan

def get_histogram_data(campaigns, players):
    data = {}
    for player in players:
        player_dict = campaigns[player]

        data[player] = {}
        data[player]['impressions'] = extract_array(player_dict, 'impressions')
        data[player]['reach'] = extract_array(player_dict, 'reach')
        data[player]['er'] = np.vectorize(effective_reach)(data[player]['impressions'], data[player]['reach'])
        data[player]['budget'] = extract_array(player_dict, 'budget')
        data[player]['cost'] = extract_array(player_dict, 'cost')
        data[player]['profit'] = data[player]['er']*data[player]['budget'] - data[player]['cost']
        data[player]['budget/reach'] = data[player]['budget']/data[player]['reach']
        data[player]['duration'] = extract_array(player_dict, 'endDay') - extract_array(player_dict, 'startDay') + 1

        data[player]['inds'] = {}
        data[player]['inds']['All'] = np.ones_like(data[player]['profit'], dtype=bool)
        data[player]['inds']['Profitable'] = data[player]['profit'] > 0
        data[player]['inds']['Neg. Profit'] = data[player]['profit'] < 0
        data[player]['inds']['0 Profit'] = data[player]['profit'] == 0

    return data


# Map player to (500, 11) with number of active campaigns
def get_num_campaigns(campaigns, players):
    data = {}
    for player in players:
        player_dict = campaigns[player]
        data[player] = np.zeros((500, 11))
        for game_num in range(500):
            for id in player_dict[game_num]:
                start = player_dict[game_num][id]['startDay']
                end = player_dict[game_num][id]['endDay']
                data[player][game_num, start:(end+1)] += 1

    return data


# [0, 1.38442]
def effective_reach(impressions, reach):
    a = 4.08577
    b = 3.08577
    er = (2/a) * (atan(a*(impressions/reach) - b) - atan(-b))
    er = round(er, 3)
    return er


def extract_array(player_campaigns, attribte):
    num_campaigns = 0
    for game_num in range(500):
        num_campaigns += len(player_campaigns[game_num]) 
    output = np.zeros(num_campaigns)

    count = 0
    for game_num in range(500):
        for id in player_campaigns[game_num]:
            output[count] += player_campaigns[game_num][id][attribte]
            count += 1

    return output

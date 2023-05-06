import numpy as np
from math import atan

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
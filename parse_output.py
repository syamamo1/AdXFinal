from tqdm import tqdm 
import numpy as np
import re


# Return dict[player] = dict2
# dict2[attribute] = 1D/2D numpy array
def get_data(filename):
    clean(filename)
    games, final_results = get_games(filename)

    delim = ' EndOfDayMessage: '
    data, campaigns = init_data()
    # Iterate games
    for game_num, game in enumerate(games):

        # Iterate days
        for end_day_msg in game.split(delim)[1:]:

            # Iterate lines
            new_campaigns = {}
            campaigns_temp = {}
            for line in end_day_msg.split('\n'):
                
                # get Day
                if 'Day:' in line:
                    spl = line.split(':')[-1].split(',')[0]
                    day = int(spl)
          
                # get Campaign impressions, cost
                if 'Statistics:' in line:
                    if 'null' not in line: 
                        spl = line.split(':')[-1]
                        spl = re.split('{|=|\(|,|\)|}', spl)
                        spl = [x for x in spl if x not in ['', ' ']]
                        
                        for i, num in enumerate(spl):
                            if i % 3 == 0:
                                id = int(num)
                                campaigns_temp[id] = {}
                            elif i % 3 == 1:
                                impressions = int(num)
                                campaigns_temp[id]['impressions'] = impressions
                            elif i % 3 == 2:
                                cost = float(num)
                                campaigns_temp[id]['cost'] = cost

                # get Campaign reach, budget
                if '		Campaign' in line: 
                    spl = re.split('Campaign|=|\[|]|,|\t| ', line)
                    spl = [x for x in spl if x not in ['', ' ']]
                    
                    id = int(spl[0])
                    new_campaigns[id] = {}
                    new_campaigns[id]['startDay'] = int(spl[2])
                    new_campaigns[id]['endDay'] = int(spl[4])
                    new_campaigns[id]['segment'] = spl[6]
                    new_campaigns[id]['reach'] = int(spl[8])
                    new_campaigns[id]['budget'] = float(spl[10])
                
                # get Quality Score
                if 'Quality Score = ' in line:
                    spl = line.split('=')[-1]
                    quality_score = float(spl)
                
                # get Cumulative Profit
                if 'Cumulative Profit:' in line:
                    spl = line.split(':')[-1]
                    cumulative_profit = float(spl)
                
                # if 'NDaysBidBundle:' in line: pass

                # get Player
                if ' from ' in line:
                    player = line.split('from ')[-1].split('\n')[0]


            # Save data for player
            data[player]['quality_scores'][game_num, day-1] += quality_score
            data[player]['cumulative_profits'][game_num, day-1] += cumulative_profit
            save_campaign_info(campaigns, new_campaigns, campaigns_temp, player, game_num)

    return data, campaigns, final_results


# Transfer info from dicts
def save_campaign_info(campaigns, new_campaigns, campaigns_temp, player, game_num):
    
    # Start, end, segment, reach, budget
    for id in new_campaigns:
        campaigns[player][game_num][id] = {}

        campaigns[player][game_num][id]['startDay'] = new_campaigns[id]['startDay']
        campaigns[player][game_num][id]['endDay'] = new_campaigns[id]['endDay']
        campaigns[player][game_num][id]['segment'] = new_campaigns[id]['segment']
        campaigns[player][game_num][id]['reach'] = new_campaigns[id]['reach']
        campaigns[player][game_num][id]['budget'] = new_campaigns[id]['budget']

    # Impressions, cost
    for id in campaigns_temp:

        campaigns[player][game_num][id]['impressions'] = campaigns_temp[id]['impressions']
        campaigns[player][game_num][id]['cost'] = campaigns_temp[id]['cost']
    
    return campaigns


# Initialize data - dict of dicts to store game data
# Initializes campagins - dict of dicts to store campaigns
def init_data():
    data = {}
    campaigns = {}
    for i in range(0, 10):
        if i == 0:
            player = 'AlexSean'
        else:
            player = f'bot_{i}'

        # 500 games, each 11 days long
        quality_scores = np.zeros((500, 11))
        cumulative_profit = np.zeros((500, 11))

        data[player] = {}
        data[player]['quality_scores'] = quality_scores
        data[player]['cumulative_profits'] = cumulative_profit

        # Make campaigns data
        campaigns[player] = {}
        for game_num in range(500):
            campaigns[player][game_num] = {}

    return data, campaigns


# Returns list of 500 game logs
# and the table of final results
def get_games(filename):
    with open(filename) as f:
        buffer = f.read()

    delim = '''	###########################################

'''
    
    games = buffer.split(delim) # split1
    final_results = games[-2]
    games = games[:-2] # get rid of beginning

    print(final_results)
    assert len(games)==500, f'There should be 500 games. Found {len(games)}'
    return games, final_results


# "Cleans" the console file of unnecessary logs
def clean(filename):
    with open(filename, 'r') as f:
        buffer = f.read()
    lines = buffer.split('\n')

    indicator = 'ALREADY CLEANED'
    if lines[0] == indicator:
        print(f'{filename} already cleaned')
        return

    delim = '[-]'
    nred = 0
    ntot = 0
    with open(filename, 'w') as f:
        f.write(indicator + '\n')
        for line in tqdm(lines): 
            if delim not in line:
                f.write(line + '\n')
            else:
                nred += 1
            ntot += 1

    print(f'File: {filename} reduced by {round(nred/ntot, 2)*100}%')








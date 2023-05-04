from tqdm import tqdm 
import numpy as np
import re


# Return dict[player] = dict2
# dict2[attribute] = 1D/2D numpy array
def get_data(filename):
    clean(filename)
    games, final_results = get_games(filename)

    delim = ' EndOfDayMessage: '
    data = init_data()
    for i, game in enumerate(games):
        for end_day_msg in game.split(delim)[1:]:
            for line in end_day_msg.split('\n'):
                if 'Day:' in line:
                    spl = line.split(':')[-1].split(',')[0]
                    day = int(spl)

# 	Statistics: {69=(2846, 1195.756745790099), 70=(0, 0.0), 57=(0, 0.0), 74=(207, 206.99992711270198)},
#   Statistics: {76=(0, 0.0)},      
#   Statistics: null,
          
                # if 'Statistics:' in line:
                #     total_impressions = 0

                #     if 'null' not in line: 
                #         spl = line.split(':')[-1]
                #         spl = re.split('{|=|\(|,|\)|}', spl)
                #         spl = [x for x in spl if x not in ['', ' ']]
                        
                #         for i, num in enumerate(spl):
                            

                # if 'New campaigns:' in line: pass
                
                if 'Quality Score = ' in line:
                    spl = line.split('=')[-1]
                    quality_score = float(spl)
                
                if 'Cumulative Profit:' in line:
                    spl = line.split(':')[-1]
                    cumulative_profit = float(spl)
                
                # if 'NDaysBidBundle:' in line: pass

                if ' from ' in line:
                    player = line.split('from ')[-1].split('\n')[0]

            data[player]['quality_scores'][i, day-1] += quality_score
            data[player]['cumulative_profits'][i, day-1] += cumulative_profit


    return data


# Initialize dict of dicts to store 
# game data
def init_data():
    data = {}
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
        data[player]['total_reach'] = 0
        data[player]['total_impressions'] = 0
        data[player]['total_cost'] = 0

    return data


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








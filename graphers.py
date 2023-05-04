import numpy as np
import matplotlib.pyplot as plt



def graph_performance(data):
    _, (ax1, ax2) = plt.subplots(2, 1, sharex=True, figsize=(8, 8))
    
    # for player in ['AlexSean', 'bot_1']:
    for i in range(0, 10):
        if i == 0:
            player = 'AlexSean'
        else:
            player = f'bot_{i}'
        x = np.arange(11) + 1
        qs = data[player]['quality_scores']
        cp = data[player]['cumulative_profits']
        qs_mean = np.mean(qs, axis=0)
        qs_std = np.std(qs, axis=0) 
        cp_mean = np.mean(cp, axis=0)
        cp_std = np.std(cp, axis=0) 

        # ax1.plot(x, qs_mean, label=player)
        ax1.errorbar(x, qs_mean, yerr=qs_std, marker = '.', linestyle = 'dashed', label=player)
        # ax2.plot(x, cp_mean, label=player)
        ax2.errorbar(x, cp_mean, yerr=cp_std, marker = '.', linestyle = 'dashed', label=player)

    ax1.set_title('Quality Score')
    ax1.legend()
    ax1.grid(True)
    ax1.set_ylim(-0.05, None)

    ax2.set_title('Cumulative Profit')
    ax2.legend()
    ax2.grid(True)
    ax2.set_xlabel('Day')

    plt.show()
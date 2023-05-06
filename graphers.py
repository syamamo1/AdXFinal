import numpy as np
import matplotlib.pyplot as plt

from utils import effective_reach, extract_array



# Histograms of campaign (impressions/reach), ER, costs
def make_histograms(campaigns, players):
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

        data[player]['inds'] = {}
        data[player]['inds']['All'] = np.ones_like(data[player]['profit'], dtype=bool)
        data[player]['inds']['Profitable'] = data[player]['profit'] > 0
        data[player]['inds']['Neg. Profit'] = data[player]['profit'] < 0
        data[player]['inds']['0 Profit'] = data[player]['profit'] == 0

    fig, axes = plt.subplots(4, 4, figsize=(8, 8))
    subgroups = ['All', 'Profitable', 'Neg. Profit', '0 Profit']
    gtypes = ['Profit', 'Impressions/Reach', 'Effective Reach', 'Cost per Impression']
    for row, (ax_row, subgroup) in enumerate(zip(axes, subgroups)):
        for col, (ax, gtype) in enumerate(zip(ax_row, gtypes)):
            for player in players:
                inds = data[player]['inds'][subgroup]

                if col == 0: # Profit
                    x = data[player]['profit']
                elif col == 1: # Impressions/Reach
                    x = data[player]['impressions']/data[player]['reach']
                elif col == 2: # Effective Reach
                    x = data[player]['er']
                elif col == 3: # Cost per Impression
                    non_z = data[player]['impressions'] != 0
                    x = data[player]['cost'][non_z]/data[player]['impressions'][non_z]
                    inds = inds[non_z]
                    
                ax.hist(x[inds], bins=20, alpha=0.5, label=player)

            ax.legend()
            if row == 0: ax.set_title(gtype)
            if col == 0: ax.set_ylabel(subgroup, rotation = 90, size='large')

    # Rows 1,2,3 share x-axis
    # Cols 2,3 share y-axis
    for j in range(axes.shape[1]):
        y_ax_list = [axes[i, j] for i in range(1, axes.shape[0]-1)]
        shared_y_axes = axes[1, j].get_shared_y_axes()
        shared_y_axes.join(*y_ax_list)

        x_ax_list = [axes[i, j] for i in range(0, axes.shape[0]-1)]
        shared_x_axes = axes[0, j].get_shared_x_axes()
        shared_x_axes.join(*x_ax_list)
                    
    fig.tight_layout()
    plt.show()


# Graph quality scores, cumulative profits
def graph_performance(data, players):
    _, (ax1, ax2) = plt.subplots(2, 1, sharex=True, figsize=(8, 8))
    
    for player in players:
        x = np.arange(11) + 1
        qs = data[player]['quality_scores']
        cp = data[player]['cumulative_profits']
        qs_mean = np.mean(qs, axis=0)
        qs_std = np.std(qs, axis=0) 
        cp_mean = np.mean(cp, axis=0)
        cp_std = np.std(cp, axis=0) 

        if player == 'AlexSean':
            ax1.errorbar(x, qs_mean, yerr=qs_std, marker = '.', linestyle = 'dashed', label=player)
            ax2.errorbar(x, cp_mean, yerr=cp_std, marker = '.', linestyle = 'dashed', label=player)
        else:
            ax1.plot(x, qs_mean, label=player)
            ax2.plot(x, cp_mean, label=player)

    ax1.set_title('Quality Score')
    ax1.legend()
    ax1.grid(True)
    ax1.set_ylim(-0.05, None)

    ax2.set_title('Cumulative Profit')
    ax2.legend()
    ax2.grid(True)
    ax2.set_xlabel('Day')

    plt.show()




# Histograms of campaign (impressions/reach), ER, costs
# Old version - only does "overall"
def make_histograms_old(campaigns, players):
    fig, axes = plt.subplots(4, 4, figsize=(8, 8))
    prefixes = ['Overall', 'Profitable', '0', 'Negative']
    for player in players:
        for i, ax_row in enumerate(axes):
            (ax1, ax2, ax3, ax4) = ax_row
            prefix = prefixes[i]
            player_dict = campaigns[player]
            impressions = extract_array(player_dict, 'impressions')
            reach = extract_array(player_dict, 'reach')
            cost = extract_array(player_dict, 'cost')

            er = np.vectorize(effective_reach)(impressions, reach)
            profit = er*reach - cost
            ax1.hist(profit, bins=20, alpha=0.5, label=player)
            ax2.hist(impressions/reach, bins=20, alpha=0.5, label=player)
            ax3.hist(er, bins=20, alpha=0.5, label=player)
            non_z = impressions != 0
            ax4.hist(cost[non_z]/impressions[non_z], bins=20, alpha=0.5, label=player)
        
        ax1.set_title(prefix + ' Profit')
        ax1.legend()

        ax2.set_title(prefix + ' Impressions/Reach')
        ax2.legend()

        ax3.set_title(prefix + ' Effective Reach')
        ax3.legend()

        ax4.set_title(prefix + ' Cost per Impression')
        ax4.legend()

    fig.subplots_adjust(hspace=0.6)
    plt.show()
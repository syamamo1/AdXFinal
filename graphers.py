import numpy as np
import matplotlib.pyplot as plt

from utils import effective_reach, extract_array, get_num_campaigns



# Breaks up into 9 bins by profit level
def make_detailed_histograms(data, players):
    # Do weight = sum(profit_in_bin)/sum(profit)

    gtypes = ['Impressions/Reach', 'Effective Reach', 'Cost per Impression', 'Budget per Reach', 'Duration']
    # bin_edges = np.histogram_bin_edges(data[players[0]]['profit'], bins=9)
    # bin_edges = np.linspace(-3200, 4000, 10, dtype=int)
    bin_edges = np.array([-3500,-1500,-500,-100,-0.00001,0.00001,100,500,1500,3500])
    fig1, axes1 = plt.subplots(5, 5, figsize=(13, 9))
    fig2, axes2 = plt.subplots(5, 5, figsize=(13, 9))
    num_bins = 9
    for player in players:
        weights = []
        bin_inds = np.digitize(data[player]['profit'], bin_edges, right=True)

        # All bin + first 4 bins
        bins = {}
        for row, ax_row in enumerate(axes1):
            for col, (ax, gtype) in enumerate(zip(ax_row, gtypes)):
                
                inds = (bin_inds == row)
                
                if col == 0: # Impressions/Reach
                    x = data[player]['impressions']/data[player]['reach']
                elif col == 1: # Effective Reach
                    x = data[player]['er']
                elif col == 2: # Cost per Impression
                    non_z = data[player]['impressions'] != 0
                    x = data[player]['cost'][non_z]/data[player]['impressions'][non_z]
                    inds = inds[non_z]
                elif col == 3: # Budget per Reach
                    x = data[player]['budget/reach']
                elif col == 4: # Duration
                    x = data[player]['duration']
                
                if row == 0: 
                    inds = np.ones_like(x, dtype=bool)
                    bins[gtype] = np.histogram_bin_edges(x, bins=20)

                ax.hist(x[inds], bins=bins[gtype], alpha=0.5, label=player, edgecolor='black')

                # ax.legend()
                if row == 0: 
                    ax.set_title(gtype)
                if col == 0: 
                    if row == 0:
                        ax.set_ylabel('All', rotation = 90, size='large')
                    else:
                        start = round(bin_edges[row-1], -1)
                        end = round(bin_edges[row], -1)
                        ax.set_ylabel(f'({end}, {start})', rotation = 90, size='large')

        # Now do next 5 bins
        bins = {}
        for row, ax_row in enumerate(axes2, start=4):
            for col, (ax, gtype) in enumerate(zip(ax_row, gtypes)):
                
                inds = (bin_inds == row+1)
                
                if col == 0: # Impressions/Reach
                    x = data[player]['impressions']/data[player]['reach']
                elif col == 1: # Effective Reach
                    x = data[player]['er']
                elif col == 2: # Cost per Impression
                    non_z = data[player]['impressions'] != 0
                    x = data[player]['cost'][non_z]/data[player]['impressions'][non_z]
                    inds = inds[non_z]
                elif col == 3: # Budget per Reach
                    x = data[player]['budget/reach']
                elif col == 4: # Duration
                    x = data[player]['duration']
                
                if row == 4: 
                    bins[gtype] = np.histogram_bin_edges(x, bins=20)

                ax.hist(x[inds], bins=bins[gtype], alpha=0.5, label=player, edgecolor='black')

                if row == 4: 
                    ax.set_title(gtype)
                if col == 0:
                    start = round(bin_edges[row], -1)
                    end = round(bin_edges[row+1], -1)
                    ax.set_ylabel(f'({end}, {start})', rotation = 90, size='large')

        # Print some stats
        total_flow = np.linalg.norm(data[player]['profit'], ord=1)
        print('Bin distribution:')
        for bin_num in range(num_bins):
            portion = np.linalg.norm(data[player]['profit'][bin_inds == bin_num+1], ord=1)
            weight = round(100*portion/total_flow, 1)
            weights.append(weight)

            start = round(bin_edges[bin_num])
            end = round(bin_edges[bin_num+1])
            num_instances = data[player]['profit'][bin_inds == bin_num+1].shape[0]
            print(f'({start}, {end}), Weight = {weight}, Instances = {num_instances}')


        # Print bins, weights, num instances
        for row, ax_row in enumerate(axes1):
            for col, (ax, gtype) in enumerate(zip(ax_row, gtypes)):
                if col == len(gtypes)-1 and player == players[0] and row > 0:
                    ax2 = ax.twinx()
                    ax2.set_ylabel(weights[row-1], rotation = 90, size='large')
                    ax2.yaxis.set_ticklabels([])
            plt.tick_params(right=False)
        for row, ax_row in enumerate(axes2, start=4):
            for col, (ax, gtype) in enumerate(zip(ax_row, gtypes)):
                if col == len(gtypes)-1 and player == players[0]:
                    ax2 = ax.twinx()
                    ax2.set_ylabel(weights[row], rotation = 90, size='large')
                    ax2.yaxis.set_ticklabels([])
            plt.tick_params(right=False)


        print('Total weight = ', round(sum(weights), 1), '\n')

        axes1[0, 0].legend()
        axes2[0, 0].legend()
        title1 = f'Weights = {[100] + weights[:4]}'
        title2 = f'Weights = {weights[4:]}'
#        fig1.canvas.set_window_title(title1)
#        fig2.canvas.set_window_title(title2)

    # Rows 1,2,3,4,5 share x-axis
    # Rows 2,3,4,5 share y-axis
    for j in range(axes1.shape[1]):
        x1_ax_list = [axes1[i, j] for i in range(0, axes1.shape[0])]
        shared_x_axes = axes1[0, j].get_shared_x_axes()
        shared_x_axes.join(*x1_ax_list)

        # y1_ax_list = [axes1[i, j] for i in range(1, axes1.shape[0])]
        # shared_y_axes = axes1[1, j].get_shared_y_axes()
        # shared_y_axes.join(*y1_ax_list)

        x2_ax_list = [axes2[i, j] for i in range(0, axes2.shape[0])]
        shared_x_axes = axes1[0, j].get_shared_x_axes()
        shared_x_axes.join(*x2_ax_list)

        # y2_ax_list = [axes2[i, j] for i in range(1, axes2.shape[0])]
        # shared_y_axes = axes1[1, j].get_shared_y_axes()
        # shared_y_axes.join(*y2_ax_list)
                    
    fig1.tight_layout()
    fig2.tight_layout()


    

# Only does all, profitable, and negative profitable
def make_general_histograms(data, players):
    fig, axes = plt.subplots(4, 6, figsize=(13, 9))
    subgroups = ['All', 'Profitable', 'Neg. Profit', '0 Profit']
    gtypes = ['Profit', 'Impressions/Reach', 'Effective Reach', 'Cost per Impression', 'Budget per Reach', 'Duration']
    bins = {}
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
                elif col == 4: # Budget per Reach
                    x = data[player]['budget/reach']
                elif col == 5: # Duration
                    x = data[player]['duration']
                
                if row == 0: 
                    bins[gtype] = np.histogram_bin_edges(x, bins=20)

                ax.hist(x[inds], bins=bins[gtype], alpha=0.5, label=player, edgecolor='black')

            if row == 0: ax.set_title(gtype)
            if col == 0: ax.set_ylabel(subgroup, rotation = 90, size='large')
            if row == 0 and col == 0: ax.legend()

    # Rows 1,2,3,4 share x-axis
    # Rows 2,3 share y-axis
    for j in range(axes.shape[1]):
        x_ax_list = [axes[i, j] for i in range(0, axes.shape[0])]
        shared_x_axes = axes[0, j].get_shared_x_axes()
        shared_x_axes.join(*x_ax_list)

        # y_ax_list = [axes[i, j] for i in range(1, axes.shape[0]-1)]
        # shared_y_axes = axes[1, j].get_shared_y_axes()
        # shared_y_axes.join(*y_ax_list)
                    
    fig.tight_layout()


# Graph quality scores, cumulative profits
def graph_performance(data, campaigns, players):
    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, sharex=True, figsize=(8, 10))
    num_campaigns = get_num_campaigns(campaigns, players)

    for player in players:
        x = np.arange(11) + 1
        qs = data[player]['quality_scores']
        cp = data[player]['cumulative_profits']
        num_c = num_campaigns[player]

        qs_mean = np.mean(qs, axis=0)
        qs_std = np.std(qs, axis=0) 
        cp_mean = np.mean(cp, axis=0)
        cp_std = np.std(cp, axis=0) 
        num_c_mean = np.mean(num_c, axis=0)
        num_c_std = np.std(num_c, axis=0) 

        if player == 'AlexSean':
            ax1.errorbar(x, cp_mean, yerr=cp_std, marker = '.', linestyle = 'dashed', label=player)
            ax2.errorbar(x, qs_mean, yerr=qs_std, marker = '.', linestyle = 'dashed', label=player)
            ax3.errorbar(x, num_c_mean, yerr=num_c_std, marker = '.', linestyle = 'dashed', label=player)
        else:
            ax1.plot(x, cp_mean, label=player)
            ax2.plot(x, qs_mean, label=player)
            ax3.plot(x, num_c_mean, label=player)

    ax1.set_title('Cumulative Profit')
    ax1.legend()
    ax1.grid(True)

    ax2.set_title('Quality Score')
    ax2.legend()
    ax2.grid(True)
    ax2.set_ylim(-0.05, None)

    ax3.set_title('Number of Campaigns')
    ax3.legend()
    ax3.grid(True)
    ax3.set_xlabel('Day')

    fig.subplots_adjust(hspace=0.5)




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

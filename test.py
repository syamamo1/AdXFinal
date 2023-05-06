# for i in range(5):
#     if True:
#         if i == 0: break
#         print('hi')
#     if True:
#         print(i)

# import re

# s = '		Campaign 78 = [startDay = 8, endDay = 9, Segment = FEMALE_YOUNG_LOW_INCOME, Reach = 1980, Budget = 741.9802882601425]'

# print(s)

# spl = re.split('Campaign|=|\[|]|,|\t| ', s)
# spl = [x for x in spl if x not in ['', ' ']]
# print(spl)
import numpy as np
a = np.zeros((5, 5))
a[1,2] += 1

print(np.where(a==1))
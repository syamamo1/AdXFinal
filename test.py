# for i in range(5):
#     if True:
#         if i == 0: break
#         print('hi')
#     if True:
#         print(i)

import re

s = 'Statistics: {69=(2846, 1195.756745790099), 70=(0, 0.0), 57=(0, 0.0), 74=(207, 206.99992711270198)},'
# s = 'Statistics: {76=(0, 0.0)},      '


spl = s.split(':')[-1]

print(spl)

spl = re.split('{|=|\(|,|\)|}', spl)
spl = [x for x in spl if x not in ['', ' ']]
print(spl)
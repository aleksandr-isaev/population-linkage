#
# Copyright 2022 Systems Research Group, University of St Andrews:
# <https://github.com/stacs-srg>
#
# This file is part of the module population-linkage.
#
# population-linkage is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
# License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
# version.
#
# population-linkage is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
# warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with population-linkage. If not, see
# <http://www.gnu.org/licenses/>.
#

import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv('../../../../../../../../../birthbirthtri2.csv')

df['distance_diff'] = df['max_distance'] - df['average_distance']

true_sibling = df[df['has_GT_SIBLING'] == True]['distance_diff']
false_sibling = df[df['has_GT_SIBLING'] == False]['distance_diff']

fig, axs = plt.subplots(1, 2, figsize=(12, 6))

axs[0].hist(false_sibling, bins=20, alpha=0.5, label='False Sibling', color='orange')
axs[0].hist(true_sibling, bins=20, alpha=0.5, label='True Sibling', color='blue')

axs[0].set_xlabel('Distance Difference (max - avg)')
axs[0].set_ylabel('Frequency')
axs[0].set_title('Histogram of Distance Difference by Sibling Status')
axs[0].legend(loc='upper right')

axs[1].hist(df['link_num'], bins=20, alpha=0.7, color='green')

axs[1].set_xlabel('Number of links')
axs[1].set_ylabel('Frequency')
axs[1].set_title('Histogram for number of links')

plt.tight_layout()
plt.show()

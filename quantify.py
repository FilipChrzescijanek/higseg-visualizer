import sys
import os, os.path

print(len([name for name in os.listdir('./' + sys.argv[1])]))
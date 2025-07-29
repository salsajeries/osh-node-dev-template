import Adafruit_DHT as dht
from time import sleep

DHT = 4

h, t = dht.read(dht.DHT22, DHT)
if h is not None and t is not None:
    print('{0:0.1f}, {1:0.1f}'.format(t, h))
else:
    print('Error reading.')
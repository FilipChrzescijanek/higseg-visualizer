import sys
import os.path
import requests
import numpy as np
from flask import Flask
from flask import request
from tensorflow.python.keras.models import load_model
from tensorflow.python.keras.preprocessing.image import ImageDataGenerator

app = Flask(__name__)

url = 'https://ln.syncusercontent.com/mfs-60:aaabff72c2cd5d178d85c56a7bb0c4a8=============================/p/higseg.h5?allowdd=0&datakey=MnKKUmSd3cXWjrv8PW6geG9wFWggux9vw+JJVE2oXBvOymkyhTqKZ2/AVsCFPi3hMywCuzq6ilDP3ja9Jmb9+2rWvUyZyvMJkwn6lru4UJeaI2eyTdWq/F5SOM20vyNuR5K12iQqK6L3djY8rJJtHgrXuaKjhilGWS8NlwTyf4574HCWlJBtkNIS7WiZ7okGpaXkJrkX0a8+6109u+kNdmk0fg/Uc40VngSLov+JMJSe9ehceRFW0N58Wr3K3uiDYBibPv/vucjFhmGSO031CvNh8DC9XsGYQVW1Ca1cGEjr8Yr/BmYtTqDt5yqnCE4Y+6Q0/nX5Bi5VBX2SetqyuA&engine=ln-2.10.11&errurl=U3gsOWPD9Cj6bLOcJ4DDVUt0QH430CwRzd3BteTm34DQVji+yvI3CGqnkHwaCqf+FKnET2eCumMozx/Tc+k4qxnHXxtivBt/OtvnTZkzEPkTPw//Y+/CPA8V/hXWQOQApqJI5QkW7/kuac/K0dy7ix6nrNsvZ/BvT3hLIt4XF/JkhFgVZeMFzyGXmg51MsUKQ0W3wYYh2LJTzWcOrqXDeym4XPQxQ8FLm/Es9xs4t0JArVzmxvvKBY9jIQZw76UEjM61egWAKXngkVnmgiFVkMw0M6afuOt14R/jcwyfH8kO99LsRX51E0qOZK1lc1lu8ibFyzoSNOLAIxmWPKiu3Q==&header1=Q29udGVudC1UeXBlOiB1bmRlZmluZWQ&header2=Q29udGVudC1EaXNwb3NpdGlvbjogYXR0YWNobWVudDsgZmlsZW5hbWU9ImhpZ3NlZy5oNSI7ZmlsZW5hbWUqPVVURi04JydoaWdzZWcuaDU7&ipaddress=a56e079a00390463c47140cfd183e2dfcf557b56&linkcachekey=ab484b310&linkoid=574310007&mode=100&sharelink_id=7366372640007&timestamp=1526182871401&uagent=b0b5474d224f5d474404f8d6c3e4605ee16bd4ce&signature=60eb4c1bd97bb1e9f478d1542dca5c54758d7d7c'

@app.route("/")
def hello():
    dir = request.args.get('dir')
    datagen = ImageDataGenerator(rescale=1./255)
    generator = datagen.flow_from_directory(dir, target_size=(56, 56), color_mode='grayscale', batch_size=1, class_mode=None, shuffle=False)
    predictions = model.predict_generator(generator)
    return str(sum(np.argmax(predictions, axis=1)) + len(predictions))

if __name__ == '__main__':
    if not os.path.isfile('higseg.h5'):
        r = requests.get(url, allow_redirects=True)
        with open('higseg.h5', 'wb') as f:
            f.write(r.content)
    model = load_model('higseg.h5')
    app.run(debug=False, port=int(sys.argv[1]))

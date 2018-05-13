import sys
import numpy as np
from flask import Flask
from flask import request
from tensorflow.python.keras.models import load_model
from tensorflow.python.keras.preprocessing.image import ImageDataGenerator

app = Flask(__name__)

@app.route("/")
def hello():
    dir = request.args.get('dir')
    datagen = ImageDataGenerator(rescale=1./255)
    generator = datagen.flow_from_directory(dir, target_size=(56, 56), color_mode='grayscale', batch_size=1, class_mode=None, shuffle=False)
    predictions = model.predict_generator(generator)
    return str(sum(np.argmax(predictions, axis=1)) + len(predictions))

if __name__ == '__main__':
    if not os.path.isfile('higseg.h5'):
        raise Error('[Errno 2] Neural network model not found: higseg.h5')
    model = load_model('higseg.h5')
    app.run(debug=False, port=int(sys.argv[1]))

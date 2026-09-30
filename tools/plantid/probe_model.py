from ai_edge_litert.interpreter import Interpreter
it = Interpreter(model_path='model/plants.tflite')
it.allocate_tensors()
for d in it.get_input_details():
    print("input :", d['shape'], d['dtype'], "quant:", d['quantization'])
for d in it.get_output_details():
    print("output:", d['shape'], d['dtype'], "quant:", d['quantization'])

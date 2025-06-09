# SampleKarateParameterization
import gliner

# Assume gliner has a default pipeline that runs a lightweight, built-in NER process.
# This pipeline does not require an external model file.
ner_pipeline = gliner.pipeline()  # Using the built-in default mode

# Sample text for Named Entity Recognition
text = "Amazon was founded by Jeff Bezos in Seattle, Washington."

# Process the text through the pipeline
entities = ner_pipeline.recognize(text)

# Print out the recognized entities
print("Recognized Entities:")
for entity in entities:
    print(f"Entity: {entity['text']}, Label: {entity['label']}")



import gliner

# Initialize the lightweight NER model
# (Assuming gliner has a 'load_model' function)
model = gliner.load_model("default")

# Sample text
text = "Amazon was founded by Jeff Bezos in Seattle, Washington."

# Run the NER process
entities = model.recognize(text)

# Print out the recognized entities
for entity in entities:
    print(f"Entity: {entity['text']}, Type: {entity['label']}, Confidence: {entity.get('score', 'N/A')}")


strict matching

# Option 1: Using multiple conditions in JSONPath
And json SequenceServer = karate.jsonPath(response.hits, '$.hits[?(@._source.text contains "Successful openshift CLI validation" && !(@._source.text contains "Failed openshift validation"))]')

# Option 2: Filter in two steps (can be clearer for complex logic)
* def successfulLogs = karate.jsonPath(response.hits, '$.hits[?(@._source.text contains "Successful openshift CLI validation")]')
And json SequenceServer = karate.jsonPath(successfulLogs, '$[?(!(@._source.text contains "Failed openshift validation"))]')

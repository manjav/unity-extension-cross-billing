using BazaarInAppBilling;
using UnityEditor;
using UnityEngine;

[CustomEditor(typeof(StoreHandler), true)]
public class StoreHandlerEditor : Editor {

    private readonly string version = "1.1";
    private SerializedProperty products, market, publicKey, bindURL, packageURL, payload, editorDummyResponse, clientId, clientSecret, refreshToken;
    
    private StoreHandler storeHandler;

    private void OnEnable()
    {
        storeHandler = target as StoreHandler;

        products = serializedObject.FindProperty("products");
        market = serializedObject.FindProperty("market");
        publicKey = serializedObject.FindProperty("publicKey");
        bindURL = serializedObject.FindProperty("bindURL");
        packageURL = serializedObject.FindProperty("packageURL");
        payload = serializedObject.FindProperty("payload");
        editorDummyResponse = serializedObject.FindProperty("editorDummyResponse");
        clientId = serializedObject.FindProperty("clientId");
        clientSecret = serializedObject.FindProperty("clientSecret");
        refreshToken = serializedObject.FindProperty("refreshToken");
    }
    
    public override void OnInspectorGUI()
    {
        serializedObject.Update();

        Settings();
        FooterInformation();

        serializedObject.ApplyModifiedProperties();
    }

    private void Settings()
    {
        EditorGUILayout.Space();
        EditorGUILayout.Space();

        EditorGUILayout.PropertyField(products, new GUIContent("products", "First define your products in Bazaar panel."), true);
        
        EditorGUILayout.PropertyField(market, new GUIContent("Market", "Select target Market."));
        
        EditorGUILayout.PropertyField(publicKey, new GUIContent("Public Key", "RSA Key from Market."));

        EditorGUILayout.PropertyField(bindURL, new GUIContent("Bind URL", "Bind URL for redirect to market app."));
        
        EditorGUILayout.PropertyField(packageURL, new GUIContent("Package URL", "Package URL for redirect to market app."));

        EditorGUILayout.PropertyField(payload, new GUIContent("Payload", "Arbitrary value to identify purchases."));


        EditorGUILayout.PropertyField(editorDummyResponse, new GUIContent("Editor Dummy Response", "If checked all the operations will call success events."));
    }

    private void FooterInformation()
    {
        EditorGUILayout.Space();
        EditorGUILayout.Space();

        GUILayout.BeginVertical("HelpBox");

        GUIStyle style = new GUIStyle(EditorStyles.label);
        style.normal.textColor = Color.black;
        style.fontSize = 20;
        style.alignment = TextAnchor.MiddleLeft;
        GUILayout.Label("Bazaar IAB Plugin", style);

        EditorGUILayout.Space();
        style.normal.textColor = Color.gray;
        style.fontSize = 18;
        GUILayout.Label("Version: " + version, style);

        GUILayout.EndVertical();
    }
    
}

[CustomPropertyDrawer(typeof(DrawIfAttribute))]
public class DrawIfPropertyDrawer : PropertyDrawer
{
    #region Fields
 
    // Reference to the attribute on the property.
    DrawIfAttribute drawIf;
 
    // Field that is being compared.
    SerializedProperty comparedField;
 
    #endregion
 
    public override float GetPropertyHeight(SerializedProperty property, GUIContent label)
    {
        if (!ShowMe(property) && drawIf.disablingType == DrawIfAttribute.DisablingType.DontDraw)
            return 0f;
   
        // The height of the property should be defaulted to the default height.
        return base.GetPropertyHeight(property, label);
    }
 
    /// <summary>
    /// Errors default to showing the property.
    /// </summary>
    private bool ShowMe(SerializedProperty property)
    {
        drawIf = attribute as DrawIfAttribute;
        // Replace propertyname to the value from the parameter
        string path = property.propertyPath.Contains(".") ? System.IO.Path.ChangeExtension(property.propertyPath, drawIf.comparedPropertyName) : drawIf.comparedPropertyName;
 
        comparedField = property.serializedObject.FindProperty(path);
 
        if (comparedField == null)
        {
            Debug.LogError("Cannot find property with name: " + path);
            return true;
        }
 
        // get the value & compare based on types
        switch (comparedField.type)
        { // Possible extend cases to support your own type
            case "bool":
                return comparedField.boolValue.Equals(drawIf.comparedValue);
            case "Enum":
                return comparedField.enumValueIndex.Equals((int)drawIf.comparedValue);
            default:
                Debug.LogError("Error: " + comparedField.type + " is not supported of " + path);
                return true;
        }
    }
 
    public override void OnGUI(Rect position, SerializedProperty property, GUIContent label)
    {
        // If the condition is met, simply draw the field.
        if (ShowMe(property))
        {
            EditorGUI.PropertyField(position, property);
        } //...check if the disabling type is read only. If it is, draw it disabled
        else if (drawIf.disablingType == DrawIfAttribute.DisablingType.ReadOnly)
        {
            GUI.enabled = false;
            EditorGUI.PropertyField(position, property);
            GUI.enabled = true;
        }
    }
 
}
 

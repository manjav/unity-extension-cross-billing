using System.Collections;
using System.Collections.Generic;
using UnityEngine;

[CreateAssetMenu(menuName = "MarketConfigs")]
public class MarketConfigs : ScriptableObject
{
    public string publicKey;
    public string bindURL;
    public string packageURL;
    public string manifestPermission;
}

using System.Collections;
using System.Collections.Generic;
using UnityEngine;

[CreateAssetMenu(fileName = "SelectedMarket", menuName = "SelectedMarket")]
public class SelectedMarket : ScriptableObject
{
    public BazaarInAppBilling.StoreHandler.StoreName storName;
}

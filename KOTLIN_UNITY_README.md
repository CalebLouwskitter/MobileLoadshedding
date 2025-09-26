# Kotlin-Unity Integration Guide

This document outlines how to connect Kotlin code in the Android project with the Unity game.

## Calling Kotlin from Unity

To call a Kotlin function from Unity, you need to use Android's JNI (Java Native Interface).

1.  **Create a Kotlin function:**
    In any of the Kotlin files under `app/src/main/java`, create a public function. For example, in `UnityBridge.kt`:

    ```kotlin
    fun showToastFromUnity(message: String) {
        // This needs a context, so you might need to pass it from your Activity
        // For simplicity, we'll just print to logcat here
        println("Message from Unity: $message")
    }
    ```

2.  **Call from Unity (C#):**
    In a C# script in your Unity project, you can call this function like this:

    ```csharp
    using UnityEngine;

    public class MyUnityScript : MonoBehaviour
    {
        public void SendMessageToKotlin()
        {
            using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                using (AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                {
                    activity.Call("showToastFromUnity", "Hello from Unity!");
                }
            }
        }
    }
    ```

## Calling Unity from Kotlin

To call a function in a Unity script from Kotlin, you can use `UnityPlayer.UnitySendMessage`.

1.  **Create a C# function in Unity:**
    In a C# script attached to a GameObject in your scene, create a public function.

    ```csharp
    using UnityEngine;

    public class MyUnityReceiver : MonoBehaviour
    {
        public void MyUnityFunction(string message)
        {
            Debug.Log("Message from Kotlin: " + message);
        }
    }
    ```

2.  **Call from Kotlin:**
    In your Kotlin code, you can call this function using the `UnityBridge.kt` helper:

    ```kotlin
    UnityBridge.sendMessageToUnity("MyGameObject", "MyUnityFunction", "Hello from Kotlin!");
    ```

    Replace `"MyGameObject"` with the name of the GameObject in your Unity scene that has the `MyUnityReceiver` script attached.

## Folder Structure

*   `app/src/main/java/com/prog7314/IdleLoadsheddingMobile/auth`: Place all Firebase Authentication related code here.
*   `app/src/main/java/com/prog7314/IdleLoadsheddingMobile/networking`: Place all Retrofit and network-related code here.
*   `app/src/main/java/com/prog7314/IdleLoadsheddingMobile/unity`: Contains the `UnityBridge` to facilitate communication between Kotlin and Unity.
*   `app/src/main/java/com/prog7314/IdleLoadsheddingMobile/MainActivity.kt`: This is the main entry point of the Android app. You can add UI elements here that overlay the Unity game.

package com.bounswe2026group8.emergencyhub.offline.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bounswe2026group8.emergencyhub.R

class FirstAidActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_aid)

        // 1. Find all four cards by their IDs from the XML
        val displacementCard = findViewById<CardView>(R.id.card_displacement)
        val checkingCard = findViewById<CardView>(R.id.card_checking)
        val cprCard = findViewById<CardView>(R.id.card_cpr)
        val abcCard = findViewById<CardView>(R.id.card_abc)

        // 2. Set click listeners to open the Detail Page with specific data
        displacementCard.setOnClickListener {
            val intent = Intent(this, FirstAidDetailActivity::class.java)
            intent.putExtra("EXTRA_TITLE", "Displacement Skills")

            val displacementText = """
                If there are dangers around (such as fire, electric dangers or others) the patient has to be moved to a safe place (if it is safe for the first aid provider to do so), where providing the required first aid procedures is possible.

                — In case of a possible severe spinal injury: when a patient seems to have a possible serious injury in the spinal cord (in the backbone, either at the neck part or the back part), that patient must not be moved except if that is necessary. When necessary, it must be done as little as possible and very carefully. These precautions avoid many risks of causing further damages for the patient's mobility in the future.

                Usually, the patient needs to end up lying down, in a face-up position, on a sufficiently firm surface (for example, on the floor, which allows you to perform the chest compressions of cardiopulmonary resuscitation).
            """.trimIndent()

            intent.putExtra("EXTRA_TEXT", displacementText)

            // UNIQUE KEYS!
            intent.putExtra("EXTRA_IMAGE_ID_1", R.drawable.rautek_maneuver)
            intent.putExtra("EXTRA_IMAGE_ID_2", R.drawable.blanket_pull)

            startActivity(intent)
        }

        checkingCard.setOnClickListener {
            val intent = Intent(this, FirstAidDetailActivity::class.java)
            intent.putExtra("EXTRA_TITLE", "Checking Skills")

            // Using triple quotes and bullet points for readability
            val checkingText = """
                Evaluate the condition of the victim, first attending to the main threats to life.

                • Check Responsiveness: Tap the patient firmly on the shoulder and shout loudly, such as: "Can you hear me?"

                • Control Severe Bleeding: If the victim has a wound that bleeds abundantly, this requires immediate treatment (begin by applying direct pressure to the wound).

                • Check Pulse: If the patient does not react, check the carotid pulse by placing two fingers on either side of the neck. Alternatively, check the radial pulse on the wrist, under the thumb.

                • Check Breathing: Place your ear near their mouth to listen/feel for air, while watching their chest to see if it rises and falls.

                CRITICAL: Do not waste too much time checking. You should take no more than 10 seconds to check for breathing and a pulse before taking further action.
            """.trimIndent()

            intent.putExtra("EXTRA_TEXT", checkingText)

            // Your two image keys
            intent.putExtra("EXTRA_IMAGE_ID_1", R.drawable.carotidian_pulse)
            intent.putExtra("EXTRA_IMAGE_ID_2", R.drawable.checking_respiration)

            startActivity(intent)
        }

        cprCard.setOnClickListener {
            val intent = Intent(this, FirstAidDetailActivity::class.java)
            intent.putExtra("EXTRA_TITLE", "Cardiopulmonary Resuscitation")
            intent.putExtra("EXTRA_TEXT", "1. Place the heel of one hand on the center of the chest.\n\n2. Place the other hand on top and interlace fingers.\n\n3. Push hard and fast (at least 2 inches deep, 100-120 beats per minute).\n\n4. Let the chest rise completely between pushes.\n\n5. If trained, provide 2 rescue breaths after every 30 compressions.")
            startActivity(intent)
        }

        abcCard.setOnClickListener {
            val intent = Intent(this, FirstAidDetailActivity::class.java)
            intent.putExtra("EXTRA_TITLE", "ABC Protocol")

            val abcText = """
                A - AIRWAY: Gently tilt the head back and lift the chin to open the airway.
                
                B - BREATHING: Look, listen, and feel for normal breathing for no more than 10 seconds.
                
                C - CIRCULATION: Check for severe bleeding. If the person is not breathing normally, begin CPR immediately.


                PRESERVING LIFE
                
                The patient must have an open airway—an unobstructed passage that allows air to travel from the mouth/nose into the lungs. Conscious people maintain this automatically, but unconscious patients may be unable to do so.

                • The Recovery Position: The patient may be placed laying on their side. This relaxes the patient, clears the tongue from the pharynx, and avoids a common cause of death: choking on regurgitated stomach contents.
                
                • Choking: The airway can become blocked by a foreign object. To dislodge it, use anti-choking methods (such as back slaps, chest thrusts, or abdominal thrusts).
                
                • Transition to CPR: Once the airway has been opened, reassess the patient's breathing. If there is no breathing, or it is abnormal (e.g., agonal breathing), initiate CPR to force air into the lungs and manually pump blood around the body.
            """.trimIndent()

            intent.putExtra("EXTRA_TEXT", abcText)

            startActivity(intent)
        }
    }
}
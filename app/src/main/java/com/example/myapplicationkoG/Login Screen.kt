import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplicationkoG.ui.theme.LightGray
import com.example.myapplicationkoG.ui.theme.NeonGreen

@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Gray) 
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = "AltThread",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )
                Text(
                    text = "Upcycle. Design. Inspire.",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))


        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(LightGray)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(25.dp))
                        .background(NeonGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Log In", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Register", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightGray)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = "Email address", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightGray)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = "Password", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(NeonGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Log In", fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "Forgot password?", color = Color.Black, fontSize = 14.sp)
            }
        }
    }
}
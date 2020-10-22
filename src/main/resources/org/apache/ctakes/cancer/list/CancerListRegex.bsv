//Numbered List||(?:^[ ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+){2,}||(?:^[ ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+)
//Alpha Sentence List||(?:^[ ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n){2,}||(?:^[ ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n)
//// Name Value List||(?:^[ ]*[^\s]+:[\t ]+(?:[^\t\r\n:]+\r?\n)+){3,}||(?:^[ ]*[^\s]+:[\t ]+(?:[^\t\r\n:]+\r?\n)+)
//Name Value List||(?:^[^\t\r\n]{2,}:[\t ]+(?:[^\t\r\n:]+\r?\n)+){3,}||(?:^[^\t\r\n]{2,}:[\t ]+(?:[^\t\r\n:]+\r?\n)+)
//Multi Column List||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||\r?\n
Mixed Column||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n[\t ]*(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||[\r\n][\t ]*(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n
ER PR Score||(?:^(?:Result|ER:|PR:|ESTROGEN|PROGESTERONE|HER\-2\/NEU  )[^\r\n]+\r?\n){3,}||\r?\n
Header||(?:^[^\t\r\n\.]+\.{3,}[^\t\r\n\.]+\r?\n){3,}||\r?\n
//Numbered||(?:^[\t ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[ ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+){2,}||^[\t ]*[\d]{1,2}(?::|\.)[\t ]+(?:(?!^[\t ]*[\d]{1,2}(?::|\.)[\t ]+)(?:[^\t\r\n]+\r?\n))+
Alpha Sentence||(?:^[\t ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n){2,}||^[\t ]*[A-Z](?::|\.)+\)?[\t ]+(?:[^\t\n\.]+(?:\.|\n))+\r?\n
Name Value||(?:^[^\r\n:0-9]{2,}:[\t ]+[^\r\n]+\r?\n)+||^[^\r\n:0-9]{2,}:[\t ]+[^\r\n]+\r?\n
//Multi Column||(?:^(?:[^\s:]+(?: [^\s:]+)*(?:\t+| {3,}))+(?:[^\s]+(?: [^\s]+)*)[\t ]*\r?\n){3,}||\r?\n
//Dash||(?:^[\t ]*-{1,3}[\t ]+(?:(?:[^\t\r\n-]+-?)+\r?\n){1,3}){2,}||^[\t ]*-{1,3}[\t ]+(?:(?:[^\t\r\n-]+-?)+\r?\n)+
Dash||(?:^[\t ]*-{1,3}[\t ]+[^\t\r\n\:\-]+\r?\n){2,}||^[\t ]*-{1,3}[\t ]+[^\t\r\n\:\-]+\r?\n
Document Header||(?:^[^\t\r\n\.]+\.{3,}[^\t\r\n\.]+\r?\n){3,}||\r?\n
Checkbox||(?:^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*\[[XYN _]*\][^\r\n]+\r?\n)+)+||^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*\[[XYN _]*\][^\r\n]+\r?\n)+
Checkline||(?:^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*_+[XYN]*_+[^\r\n]+\r?\n)+)+||^(?:[^\r\n:]{2,80}:\r?\n)?(?:[\t ]*_+[XYN]*_+[^\r\n]+\r?\n)+

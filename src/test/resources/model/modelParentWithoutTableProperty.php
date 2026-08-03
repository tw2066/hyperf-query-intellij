<?php

namespace App {
class DemoOneButActuallyUsers extends \Hyperf\Database\Model\Model {
}
class User extends DemoOneButActuallyUsers {
}
}

(new \App\User())->newQuery()->get('<caret>');

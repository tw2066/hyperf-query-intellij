<?php

namespace App {
/**
 * @method static \Hyperf\Database\Model\Builder|DemoButActuallyUsers where($column, $operator, $value)
 */
class User extends \Hyperf\Database\Model\Model
{
}
}

\App\User::where('<caret>');
